package com.bulldozer_collective.modules.crm.sync.services.hubspot;

import com.bulldozer_collective.logging.BulldozerLogger;
import com.bulldozer_collective.modules.crm.sync.services.hubspot.dto.HubSpotAssociationBatchResponse;
import com.bulldozer_collective.modules.crm.sync.services.hubspot.dto.HubSpotObject;
import com.bulldozer_collective.modules.crm.sync.services.hubspot.dto.HubSpotPagedResponse;
import com.bulldozer_collective.modules.crm.sync.services.hubspot.dto.HubSpotPipeline;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static com.bulldozer_collective.modules.crm.configurations.CrmLogContext.op;

/**
 * Thin HubSpot REST client. Every call goes through {@link HubSpotRequestExecutor}, which throttles it and
 * retries a 429 or a transient 5xx; the timeouts come from the {@code RestTemplate} bean.
 */
@Component
@RequiredArgsConstructor
public class HubSpotClient {

    private static final String BASE_URL = "https://api.hubapi.com";
    private static final int PAGE_SIZE = 100;
    private static final int BATCH_SIZE = 100;

    private final RestTemplate restTemplate;
    private final JsonMapper jsonMapper;
    private final HubSpotRequestExecutor executor;
    private final HubSpotProperties properties;
    private final BulldozerLogger logger;

    public List<HubSpotPipeline> fetchAllDealPipelines(@NonNull String accessToken) {
        var url = BASE_URL + "/crm/v3/pipelines/deals";
        var response = this.get(url, accessToken, new TypeReference<HubSpotPagedResponse<HubSpotPipeline>>() {});
        return response.results() != null ? response.results() : List.of();
    }

    /**
     * Walks every page of an object type, handing each page to {@code pageHandler}.
     *
     * <p>The page count is capped: the loop follows HubSpot's own {@code after} cursor, and a cursor that never
     * terminates — or repeats itself — would spin here forever, holding a scheduler thread with it.
     */
    public void fetchAllObjects(@NonNull String objectType,
                                @NonNull List<String> objectProperties,
                                @NonNull String accessToken,
                                @NonNull Consumer<List<HubSpotObject>> pageHandler) {
        String after = null;
        var pages = 0;

        do {
            if (++pages > this.properties.maxPagesPerObject()) {
                this.logger.error(op("CRM-002")
                                + "Stopping the %s fetch after %d pages: HubSpot's cursor is not terminating",
                        objectType, pages - 1);
                throw new IllegalStateException(
                        "HubSpot returned more than %d pages of %s".formatted(pages - 1, objectType));
            }

            var builder = UriComponentsBuilder
                    .fromUriString(BASE_URL + "/crm/v3/objects/" + objectType)
                    .queryParam("limit", PAGE_SIZE)
                    .queryParam("properties", String.join(",", objectProperties));
            if (after != null) {
                builder.queryParam("after", after);
            }

            var response = this.get(builder.toUriString(), accessToken,
                    new TypeReference<HubSpotPagedResponse<HubSpotObject>>() {});

            if (response.results() != null && !response.results().isEmpty()) {
                pageHandler.accept(response.results());
            }

            var next = response.paging() != null ? response.paging().next() : null;
            var nextAfter = next != null ? next.after() : null;
            // A cursor pointing back at itself is the other way this loop never ends.
            after = nextAfter != null && !nextAfter.equals(after) ? nextAfter : null;
        } while (after != null);
    }

    /** Returns {@code fromId → [toId]}, in HubSpot's own ids on both sides. */
    public Map<String, List<String>> batchGetAssociations(@NonNull String fromType,
                                                          @NonNull String toType,
                                                          @NonNull List<String> fromIds,
                                                          @NonNull String accessToken) {
        var result = new HashMap<String, List<String>>();
        if (fromIds.isEmpty()) {
            return result;
        }

        for (int i = 0; i < fromIds.size(); i += BATCH_SIZE) {
            var batch = fromIds.subList(i, Math.min(i + BATCH_SIZE, fromIds.size()));
            var url = BASE_URL + "/crm/v3/associations/" + fromType + "/" + toType + "/batch/read";
            var body = this.jsonMapper.writeValueAsString(
                    Map.of("inputs", batch.stream().map(id -> Map.of("id", id)).toList()));

            var headers = authHeaders(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            var response = this.executor.execute("associations %s → %s".formatted(fromType, toType), () -> {
                var raw = this.restTemplate.exchange(
                        url, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
                return raw.getBody() == null
                        ? null
                        : this.jsonMapper.readValue(raw.getBody(), HubSpotAssociationBatchResponse.class);
            });

            if (response == null || response.results() == null) {
                continue;
            }
            for (var association : response.results()) {
                if (association.from() != null && association.to() != null) {
                    result.put(association.from().id(),
                            association.to().stream().map(target -> target.id()).toList());
                }
            }
        }

        return result;
    }

    private <T> T get(@NonNull String url,
                      @NonNull String accessToken,
                      @NonNull TypeReference<T> typeRef) {
        return this.executor.execute(url, () -> {
            var response = this.restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(authHeaders(accessToken)), String.class);
            return this.jsonMapper.readValue(response.getBody(), typeRef);
        });
    }

    private static HttpHeaders authHeaders(@NonNull String accessToken) {
        var headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        return headers;
    }
}
