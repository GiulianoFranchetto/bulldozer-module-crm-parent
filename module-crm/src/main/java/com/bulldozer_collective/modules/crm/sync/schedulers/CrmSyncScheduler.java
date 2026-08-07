package com.bulldozer_collective.modules.crm.sync.schedulers;

import com.bulldozer_collective.logging.BulldozerLogger;
import com.bulldozer_collective.modules.crm.connections.services.CrmConnectionFetcher;
import com.bulldozer_collective.modules.crm.sync.services.CrmSyncOrchestrator;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import static com.bulldozer_collective.modules.crm.configurations.CrmLogContext.op;

/**
 * Enqueues one sync job per active CRM connection, twice a day. The work itself is carried out by
 * {@link CrmSyncJobScheduler}, so this only writes rows and never blocks on a remote call.
 */
@Component
@RequiredArgsConstructor
public class CrmSyncScheduler {

    private final CrmConnectionFetcher connectionFetcher;
    private final CrmSyncOrchestrator orchestrator;
    private final BulldozerLogger logger;

    @Scheduled(cron = "0 0 6,18 * * *", zone = "UTC")
    public void syncAll() {
        var connections = this.connectionFetcher.fetchAllActive();
        this.logger.info(op("CRM-001") + "Enqueuing scheduled CRM sync for %d connection(s)", connections.size());
        for (var connection : connections) {
            try {
                this.orchestrator.enqueue(
                        connection.getCustomerId(),
                        connection.getProjectId(),
                        connection.getPlatform()
                );
            } catch (Exception e) {
                // One bad connection must not stop the batch.
                this.logger.error(op("CRM-001", connection.getCustomerId(), connection.getProjectId())
                        + "Could not enqueue the scheduled sync of connection %s",
                        connection.getId(), e);
            }
        }
    }
}
