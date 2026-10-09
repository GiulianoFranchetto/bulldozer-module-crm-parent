package com.bulldozer_collective.modules.crm.gateway;

import com.bulldozer_collective.shared.IntegrationPlatform;
import lombok.Builder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * Everything a {@link CrmGateway} needs to reach one connected CRM for the length of one request. The access
 * token is fetched from the modulith per request and never stored here.
 *
 * @param instanceUrl the CRM's own API host when it is per tenant (Salesforce), {@code null} otherwise.
 */
@Builder
public record CrmSession(@NonNull UUID customerId,
                         @NonNull UUID projectId,
                         @NonNull UUID connectionId,
                         @NonNull IntegrationPlatform platform,
                         @NonNull String accessToken,
                         @Nullable String instanceUrl) {

    @Override
    public String toString() {
        // Never let the token reach a log line through an accidental toString().
        return "CrmSession[connectionId=" + this.connectionId + ", platform=" + this.platform + "]";
    }
}
