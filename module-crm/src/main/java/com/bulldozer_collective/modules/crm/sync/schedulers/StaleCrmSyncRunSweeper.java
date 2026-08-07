package com.bulldozer_collective.modules.crm.sync.schedulers;

import com.bulldozer_collective.logging.BulldozerLogger;
import com.bulldozer_collective.modules.crm.api.CrmSyncStatus;
import com.bulldozer_collective.modules.crm.sync.data.CrmSyncRunRepository;
import com.bulldozer_collective.modules.crm.sync.services.CrmSyncRunRef;
import com.bulldozer_collective.modules.crm.sync.services.CrmSyncRunUpdater;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

import static com.bulldozer_collective.modules.crm.configurations.CrmLogContext.op;

/**
 * Closes runs stranded at {@code RUNNING}. A run only reaches a terminal status through the job state
 * listener, so a service killed mid-sync leaves the row RUNNING forever and every later report reads as if a
 * sync were still in flight. Mirrors {@code StalePublicAdImportSweeper} in the ads module.
 *
 * <p>The timeout is generous on purpose: the chain spans four poller ticks and a large portal takes a while,
 * while the scheduled sync only runs twice a day — so nothing legitimate lives this long.
 */
@Component
@RequiredArgsConstructor
public class StaleCrmSyncRunSweeper {

    private static final Duration STALE_TIMEOUT = Duration.ofHours(6);

    private final CrmSyncRunRepository repository;
    private final CrmSyncRunUpdater runUpdater;
    private final BulldozerLogger logger;

    @EventListener(ApplicationReadyEvent.class)
    @Scheduled(fixedDelay = 60 * 60 * 1000)
    public void sweep() {
        var cutoff = Instant.now().minus(STALE_TIMEOUT);
        var stale = this.repository.findAllByStatusAndStartedAtBefore(CrmSyncStatus.CRM_SYNC_STATUS_RUNNING, cutoff);
        if (stale.isEmpty()) {
            return;
        }

        this.logger.warn(op("CRM-007") + "Closing %d CRM sync run(s) stranded at RUNNING since before %s",
                stale.size(), cutoff);
        for (var run : stale) {
            this.runUpdater.finish(
                    new CrmSyncRunRef(run.getCustomerId(), run.getProjectId(), run.getId()),
                    CrmSyncStatus.CRM_SYNC_STATUS_FAILED,
                    "Sync did not complete within %s".formatted(STALE_TIMEOUT));
        }
    }
}
