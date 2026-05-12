package com.bulldozer_collective.modules.crm.pipelines.services;

import com.bulldozer_collective.shared.IntegrationPlatform;
import com.bulldozer_collective.modules.crm.pipelines.data.CrmPipelineEntity;
import com.bulldozer_collective.modules.crm.pipelines.data.CrmPipelineRepository;
import com.bulldozer_collective.modules.crm.sync.services.PlatformTokenFetcher;
import com.bulldozer_collective.modules.crm.sync.services.hubspot.HubSpotSyncService;
import com.bulldozer_collective.modules.crm.sync.services.pipedrive.PipedriveSyncService;
import com.bulldozer_collective.modules.crm.sync.services.salesforce.SalesforceSyncService;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CrmPipelineDiscoveryService {

    private final PlatformTokenFetcher tokenFetcher;
    private final HubSpotSyncService hubSpotSyncService;
    private final SalesforceSyncService salesforceSyncService;
    private final PipedriveSyncService pipedriveSyncService;
    private final CrmPipelineRepository pipelineRepository;

    public List<CrmPipelineEntity> discoverAndFetch(@NonNull UUID customerId,
                                                    @NonNull UUID projectId,
                                                    @NonNull IntegrationPlatform platform) throws Exception {
        var token = this.tokenFetcher.fetchToken(customerId, projectId, platform);
        var service = switch (platform) {
            case INTEGRATION_PLATFORM_HUBSPOT -> this.hubSpotSyncService;
            case INTEGRATION_PLATFORM_SALESFORCE -> this.salesforceSyncService;
            case INTEGRATION_PLATFORM_PIPEDRIVE -> this.pipedriveSyncService;
            default -> throw new UnsupportedOperationException("No pipeline discovery for platform: " + platform);
        };
        service.discoverPipelines(customerId, projectId, token);
        return this.pipelineRepository.findAllByCustomerIdAndProjectIdAndPlatformAndDeletedAtIsNull(
                customerId, projectId, platform);
    }
}
