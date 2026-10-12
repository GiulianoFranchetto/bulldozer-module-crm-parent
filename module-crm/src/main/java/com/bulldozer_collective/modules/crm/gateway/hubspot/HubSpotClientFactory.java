package com.bulldozer_collective.modules.crm.gateway.hubspot;

import com.bulldozer_collective.modules.crm.gateway.CrmSession;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.json.JsonMapper;

/** Builds a {@link HubSpotClient} bound to one session: its token, and its account's share of the throttle. */
@Service
@RequiredArgsConstructor
public class HubSpotClientFactory {

    private final RestTemplate restTemplate;
    private final JsonMapper jsonMapper;
    private final HubSpotRequestExecutor executor;

    public @NonNull HubSpotClient create(@NonNull CrmSession session) {
        return new HubSpotClient(this.restTemplate, this.jsonMapper, this.executor, session.accessToken(),
                session.connectionId().toString());
    }
}
