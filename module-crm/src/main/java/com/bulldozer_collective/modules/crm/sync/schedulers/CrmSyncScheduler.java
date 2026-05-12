package com.bulldozer_collective.modules.crm.sync.schedulers;

import com.bulldozer_collective.logging.BulldozerLogger;
import com.bulldozer_collective.modules.crm.connections.services.CrmConnectionFetcher;
import com.bulldozer_collective.modules.crm.sync.services.CrmSyncOrchestrator;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CrmSyncScheduler {

    private final CrmConnectionFetcher connectionFetcher;
    private final CrmSyncOrchestrator orchestrator;
    private final BulldozerLogger logger;

    @Scheduled(cron = "0 0 6,18 * * *")
    public void syncAll() {
        var connections = this.connectionFetcher.fetchAllActive();
        this.logger.info("Starting scheduled CRM sync for %d connections".formatted(connections.size()));
        for (var connection : connections) {
            try {
                this.orchestrator.sync(
                        connection.getCustomerId(),
                        connection.getProjectId(),
                        connection.getPlatform()
                );
            } catch (Exception e) {
                this.logger.error("Scheduled sync failed for connection %s".formatted(connection.getId()), e);
            }
        }
    }
}
