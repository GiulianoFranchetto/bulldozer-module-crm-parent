package com.bulldozer_collective.modules.crm.sync.services;

import com.bulldozer_collective.modules.crm.sync.services.hubspot.HubSpotSyncService;
import com.bulldozer_collective.modules.crm.sync.services.pipedrive.PipedriveSyncService;
import com.bulldozer_collective.modules.crm.sync.services.salesforce.SalesforceSyncService;
import com.bulldozer_collective.shared.IntegrationPlatform;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CrmSyncPlatformResolver {

    private final HubSpotSyncService hubSpotSyncService;
    private final SalesforceSyncService salesforceSyncService;
    private final PipedriveSyncService pipedriveSyncService;

    public CrmPlatformSyncService resolve(@NonNull IntegrationPlatform platform) {
        return switch (platform) {
            case INTEGRATION_PLATFORM_HUBSPOT -> this.hubSpotSyncService;
            case INTEGRATION_PLATFORM_SALESFORCE -> this.salesforceSyncService;
            case INTEGRATION_PLATFORM_PIPEDRIVE -> this.pipedriveSyncService;
            default -> throw new IllegalArgumentException(
                    "platform: '%s' is not a CRM platform; accepted values: %s, %s, %s".formatted(
                            platform,
                            IntegrationPlatform.INTEGRATION_PLATFORM_HUBSPOT,
                            IntegrationPlatform.INTEGRATION_PLATFORM_SALESFORCE,
                            IntegrationPlatform.INTEGRATION_PLATFORM_PIPEDRIVE));
        };
    }
}
