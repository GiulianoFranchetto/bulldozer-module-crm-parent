package com.bulldozer_collective.modules.crm.gateway.hubspot;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.json.JsonMapper;

/** Builds a {@link HubSpotClient} bound to one access token. */
@Service
@RequiredArgsConstructor
public class HubSpotClientFactory {

    private final RestTemplate restTemplate;
    private final JsonMapper jsonMapper;
    private final HubSpotRequestExecutor executor;

    public @NonNull HubSpotClient create(@NonNull String accessToken) {
        return new HubSpotClient(this.restTemplate, this.jsonMapper, this.executor, accessToken);
    }
}
