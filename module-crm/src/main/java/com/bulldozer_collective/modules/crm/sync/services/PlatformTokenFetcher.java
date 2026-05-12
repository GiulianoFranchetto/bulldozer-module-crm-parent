package com.bulldozer_collective.modules.crm.sync.services;

import com.bulldozer_collective.logging.BulldozerLogger;
import com.bulldozer_collective.projects.api.clients.InternalGetProjectIntegrationTokenClient;
import com.bulldozer_collective.shared.IntegrationPlatform;
import com.bulldozer_collective.shared.OAuthToken;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlatformTokenFetcher {

    private final BulldozerLogger logger;

    public OAuthToken fetchToken(@NonNull UUID customerId,
                                 @NonNull UUID projectId,
                                 @NonNull IntegrationPlatform platform) throws NoTokenException {
        try {
            var resp = InternalGetProjectIntegrationTokenClient.builder()
                    .customerId(customerId)
                    .projectId(projectId)
                    .platform(platform)
                    .build()
                    .execute();
            if (resp == null) {
                throw new RuntimeException("Token fetch returned null for platform " + platform);
            }
            return resp;
        } catch (RuntimeException e) {
            this.logger.error("Could not get token", e);
            throw new NoTokenException(customerId, projectId, platform);
        }
    }
}
