package com.bulldozer_collective.modules.crm.gateway;

import com.bulldozer_collective.logging.BulldozerLogger;
import com.bulldozer_collective.projects.api.clients.InternalGetProjectIntegrationTokenClient;
import com.bulldozer_collective.shared.IntegrationPlatform;
import com.bulldozer_collective.shared.OAuthToken;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static com.bulldozer_collective.modules.crm.configurations.CrmLogContext.op;

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
            this.logger.error(op("CRM-005", customerId, projectId)
                    + "Could not get token for platform %s", platform, e);
            throw new NoTokenException(customerId, projectId, platform);
        }
    }
}
