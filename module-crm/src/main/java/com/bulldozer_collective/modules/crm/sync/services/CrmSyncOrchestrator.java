package com.bulldozer_collective.modules.crm.sync.services;

import com.bulldozer_collective.logging.BulldozerLogger;
import com.bulldozer_collective.shared.IntegrationPlatform;
import com.bulldozer_collective.modules.crm.api.CrmSyncStatus;
import com.bulldozer_collective.modules.crm.sync.data.CrmSyncRunEntity;
import com.bulldozer_collective.modules.crm.sync.data.CrmSyncRunRepository;
import com.bulldozer_collective.modules.crm.sync.services.hubspot.HubSpotSyncService;
import com.bulldozer_collective.modules.crm.sync.services.pipedrive.PipedriveSyncService;
import com.bulldozer_collective.modules.crm.sync.services.salesforce.SalesforceSyncService;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CrmSyncOrchestrator {

    private final CrmSyncValidator validator;
    private final CrmSyncRunFactory syncRunFactory;
    private final CrmSyncRunRepository syncRunRepository;
    private final PlatformTokenFetcher tokenFetcher;
    private final HubSpotSyncService hubSpotSyncService;
    private final SalesforceSyncService salesforceSyncService;
    private final PipedriveSyncService pipedriveSyncService;
    private final BulldozerLogger logger;

    public CrmSyncRunEntity sync(@NonNull UUID customerId,
                                 @NonNull UUID projectId,
                                 @NonNull IntegrationPlatform platform) {
        var run = this.syncRunFactory.create(customerId, projectId, platform);
        try {
            var token = this.tokenFetcher.fetchToken(customerId, projectId, platform);
            var service = resolveService(platform);
            service.discoverPipelines(customerId, projectId, token);
            this.validator.validate(customerId, projectId, platform);
            var result = service.sync(customerId, projectId, token);

            run.setStatus(CrmSyncStatus.SUCCESS);
            run.setFinishedAt(Instant.now());
            run.setContactsSynced(result.getContactsSynced());
            run.setCompaniesSynced(result.getCompaniesSynced());
            run.setDealsSynced(result.getDealsSynced());
            run.setActivitiesSynced(result.getActivitiesSynced());
        } catch (CrmSyncValidationException e) {
            run.setStatus(CrmSyncStatus.FAILED);
            run.setFinishedAt(Instant.now());
            run.setErrorMessage(e.getMessage());
            this.logger.warn("Sync blocked by unmapped stages", e);
        } catch (NoTokenException e) {
            run.setStatus(CrmSyncStatus.FAILED);
            run.setFinishedAt(Instant.now());
            run.setErrorMessage(e.getMessage());
            this.logger.error("No token available for sync", e);
        } catch (Exception e) {
            run.setStatus(CrmSyncStatus.FAILED);
            run.setFinishedAt(Instant.now());
            run.setErrorMessage(e.getMessage());
            this.logger.error("Sync failed with unexpected error", e);
        }

        return this.syncRunRepository.save(run);
    }

    private CrmPlatformSyncService resolveService(@NonNull IntegrationPlatform platform) {
        return switch (platform) {
            case INTEGRATION_PLATFORM_HUBSPOT -> this.hubSpotSyncService;
            case INTEGRATION_PLATFORM_SALESFORCE -> this.salesforceSyncService;
            case INTEGRATION_PLATFORM_PIPEDRIVE -> this.pipedriveSyncService;
            default -> throw new UnsupportedOperationException("No CRM sync service for platform: " + platform);
        };
    }
}
