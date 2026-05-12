package com.bulldozer_collective.modules.crm.sync.services.hubspot;

import com.bulldozer_collective.modules.crm.sync.services.hubspot.dto.HubSpotAssociationBatchResponse;
import com.bulldozer_collective.modules.crm.sync.services.hubspot.dto.HubSpotObject;
import com.bulldozer_collective.modules.crm.sync.services.hubspot.dto.HubSpotPagedResponse;
import com.bulldozer_collective.modules.crm.sync.services.hubspot.dto.HubSpotPipeline;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
public class HubSpotClient {

    private static final String BASE_URL = "https://api.hubapi.com";
    private static final int PAGE_SIZE = 100;
    private static final int BATCH_SIZE = 100;

    private final RestTemplate restTemplate;
    private final JsonMapper jsonMapper;

    public List<HubSpotPipeline> fetchAllDealPipelines(@NonNull String accessToken) {
        var url = BASE_URL + "/crm/v3/pipelines/deals";
        var response = exchange(url, accessToken, new TypeReference<HubSpotPagedResponse<HubSpotPipeline>>() {});
        return response.results() != null ? response.results() : List.of();
    }

    public void fetchAllObjects(@NonNull String objectType,
                                @NonNull List<String> properties,
                                @NonNull String accessToken,
                                @NonNull Consumer<List<HubSpotObject>> pageHandler) {
        String after = null;
        do {
            var builder = UriComponentsBuilder
                    .fromHttpUrl(BASE_URL + "/crm/v3/objects/" + objectType)
                    .queryParam("limit", PAGE_SIZE)
                    .queryParam("properties", String.join(",", properties));
            if (after != null) {
                builder.queryParam("after", after);
            }
            var response = exchange(builder.toUriString(), accessToken,
                    new TypeReference<HubSpotPagedResponse<HubSpotObject>>() {});

            if (response.results() != null && !response.results().isEmpty()) {
                pageHandler.accept(response.results());
            }

            after = response.paging() != null && response.paging().next() != null
                    ? response.paging().next().after()
                    : null;
        } while (after != null);
    }

    // Returns map of fromId → list of toIds
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
            var inputs = batch.stream().map(id -> Map.of("id", id)).toList();
            var body = Map.of("inputs", inputs);

            var url = BASE_URL + "/crm/v3/associations/" + fromType + "/" + toType + "/batch/read";
            var headers = authHeaders(accessToken);
            headers.set("Content-Type", "application/json");

            try {
                var bodyJson = this.jsonMapper.writeValueAsString(body);
                var entity = new HttpEntity<>(bodyJson, headers);
                var raw = this.restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
                if (raw.getBody() != null) {
                    var response = this.jsonMapper.readValue(raw.getBody(), HubSpotAssociationBatchResponse.class);
                    if (response.results() != null) {
                        for (var assoc : response.results()) {
                            if (assoc.from() != null && assoc.to() != null) {
                                result.put(assoc.from().id(),
                                        assoc.to().stream().map(t -> t.id()).toList());
                            }
                        }
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to fetch associations %s → %s".formatted(fromType, toType), e);
            }
        }

        return result;
    }

    private <T> T exchange(@NonNull String url,
                           @NonNull String accessToken,
                           @NonNull TypeReference<T> typeRef) {
        var entity = new HttpEntity<>(authHeaders(accessToken));
        var response = this.restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        try {
            return this.jsonMapper.readValue(response.getBody(), typeRef);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse HubSpot response from " + url, e);
        }
    }

    private HttpHeaders authHeaders(@NonNull String accessToken) {
        var headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        return headers;
    }
}
