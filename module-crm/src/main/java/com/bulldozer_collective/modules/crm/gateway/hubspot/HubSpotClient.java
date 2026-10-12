package com.bulldozer_collective.modules.crm.gateway.hubspot;

import com.bulldozer_collective.modules.crm.gateway.CrmAuthorizationException;
import com.bulldozer_collective.modules.crm.gateway.hubspot.dto.HubSpotError;
import com.bulldozer_collective.shared.exceptions.BadRequestException;
import com.bulldozer_collective.shared.exceptions.ConflictException;
import com.bulldozer_collective.shared.exceptions.NotFoundException;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * HubSpot REST client bound to one access token and one connection, built per request by
 * {@link HubSpotClientFactory}. Every call goes through {@link HubSpotRequestExecutor} (the connection's throttle +
 * retry); the timeouts come from the {@code RestTemplate}.
 *
 * <p>Targets HubSpot's date-versioned API {@value #API_VERSION}: the semantic {@code /crm/v3} paths lose support in
 * September 2027.
 *
 * <p>HubSpot's {@code 4xx} are translated into the module's exceptions, carrying HubSpot's own message, so the
 * caller learns what the CRM rejected — a duplicate email, an unknown property, a value outside an enumeration.
 */
public class HubSpotClient {

    static final String API_VERSION = "2026-09";
    private static final String BASE_URL = "https://api.hubapi.com";

    private final RestTemplate restTemplate;
    private final JsonMapper jsonMapper;
    private final HubSpotRequestExecutor executor;
    private final String accessToken;
    /** The key of the throttle: the connection id, standing for the HubSpot account whose quota it draws on. */
    private final String account;

    HubSpotClient(@NonNull RestTemplate restTemplate,
                  @NonNull JsonMapper jsonMapper,
                  @NonNull HubSpotRequestExecutor executor,
                  @NonNull String accessToken,
                  @NonNull String account) {
        this.restTemplate = restTemplate;
        this.jsonMapper = jsonMapper;
        this.executor = executor;
        this.accessToken = accessToken;
        this.account = account;
    }

    /**
     * @param segments path segments after the host, each encoded on its own — an id coming from the caller can
     *                 never reshape the path.
     */
    public <T> T get(@NonNull List<String> segments,
                     @NonNull Map<String, String> query,
                     @NonNull TypeReference<T> type) {
        return this.exchange(HttpMethod.GET, segments, query, null, type, true);
    }

    /**
     * A write. A {@code POST} creates a record, so it is never replayed after a timeout or a {@code 5xx}: HubSpot
     * may have created it already, and a replay would duplicate it in the customer's CRM.
     */
    public <T> T send(@NonNull HttpMethod method,
                      @NonNull List<String> segments,
                      @NonNull Object body,
                      @NonNull TypeReference<T> type) {
        return this.exchange(method, segments, Map.of(), body, type, method != HttpMethod.POST);
    }

    /** A {@code POST …/search}: a read, replayed like any other. */
    public <T> T search(@NonNull List<String> segments,
                        @NonNull Object body,
                        @NonNull TypeReference<T> type) {
        return this.exchange(HttpMethod.POST, segments, Map.of(), body, type, true);
    }

    /** For the calls whose answer carries nothing the bridge reads (associations, by {@code PUT} / {@code DELETE}). */
    public void call(@NonNull HttpMethod method, @NonNull List<String> segments) {
        this.exchange(method, segments, Map.of(), null, null, method != HttpMethod.POST);
    }

    private <T> @Nullable T exchange(@NonNull HttpMethod method,
                                     @NonNull List<String> segments,
                                     @NonNull Map<String, String> query,
                                     @Nullable Object body,
                                     @Nullable TypeReference<T> type,
                                     boolean idempotent) {
        var uri = uri(segments, query);
        var headers = new HttpHeaders();
        headers.setBearerAuth(this.accessToken);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        String payload = null;
        if (body != null) {
            headers.setContentType(MediaType.APPLICATION_JSON);
            payload = this.jsonMapper.writeValueAsString(body);
        }
        var entity = new HttpEntity<>(payload, headers);

        try {
            // The description names the method and path only: never the query string, never the token.
            var raw = this.executor.execute(this.account, method + " " + uri.getPath(), idempotent,
                    () -> this.restTemplate.exchange(uri, method, entity, String.class).getBody());
            return type == null || raw == null || raw.isBlank() ? null : this.jsonMapper.readValue(raw, type);
        } catch (HttpClientErrorException e) {
            throw this.translate(e);
        }
    }

    private RuntimeException translate(@NonNull HttpClientErrorException e) {
        var message = "HubSpot: " + this.message(e);
        return switch (e.getStatusCode().value()) {
            case 400, 422 -> new BadRequestException(message);
            case 401, 403 -> new CrmAuthorizationException(
                    message + " — reconnect HubSpot, or grant the missing scope to the Bulldozer app");
            case 404 -> new NotFoundException(message);
            case 409 -> new ConflictException(message);
            // 429 after the executor's retries, and anything unexpected: an upstream failure, answered 502.
            default -> e;
        };
    }

    private String message(@NonNull HttpClientErrorException e) {
        try {
            var error = this.jsonMapper.readValue(e.getResponseBodyAsString(), HubSpotError.class);
            if (error != null && error.message() != null && !error.message().isBlank()) {
                return error.message();
            }
        } catch (RuntimeException ignored) {
            // Not a HubSpot error body: fall back to the status line.
        }
        return e.getStatusText();
    }

    private static URI uri(@NonNull List<String> segments, @NonNull Map<String, String> query) {
        var builder = UriComponentsBuilder.fromUriString(BASE_URL).pathSegment(segments.toArray(String[]::new));
        query.forEach(builder::queryParam);
        return builder.encode().build().toUri();
    }
}
