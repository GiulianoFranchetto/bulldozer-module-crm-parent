package com.bulldozer_collective.modules.crm.sync.schedulers;

import com.bulldozer_collective.jobs.lib.Job;
import com.bulldozer_collective.jobs.lib.JobExecutor;
import com.bulldozer_collective.jobs.lib.JobJdbcRepository;
import com.bulldozer_collective.jobs.lib.JobState;
import com.bulldozer_collective.logging.BulldozerLogger;
import com.bulldozer_collective.modules.crm.api.CrmSyncStatus;
import com.bulldozer_collective.modules.crm.sync.data.CrmSyncRunRepository;
import com.bulldozer_collective.modules.crm.sync.services.CrmSyncRunRef;
import com.bulldozer_collective.modules.crm.sync.services.CrmSyncRunUpdater;
import com.bulldozer_collective.modules.crm.sync.services.CrmSyncTaskRunner;
import org.jspecify.annotations.NonNull;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import static com.bulldozer_collective.modules.crm.configurations.CrmLogContext.op;

/**
 * Polls the CRM sync jobs and mirrors each job's terminal state onto its {@code CrmSyncRunEntity}.
 */
@Component
public class CrmSyncJobScheduler {

    private final JobExecutor executor;
    private final CrmSyncTaskRunner runner;
    private final CrmSyncRunRepository runRepository;
    private final CrmSyncRunUpdater runUpdater;
    private final BulldozerLogger logger;

    public CrmSyncJobScheduler(@NonNull JobJdbcRepository repository,
                               @NonNull CrmSyncTaskRunner runner,
                               @NonNull CrmSyncRunRepository runRepository,
                               @NonNull CrmSyncRunUpdater runUpdater,
                               @NonNull BulldozerLogger logger) {
        this.executor = new JobExecutor(repository);
        this.runner = runner;
        this.runRepository = runRepository;
        this.runUpdater = runUpdater;
        this.logger = logger;
    }

    @Scheduled(fixedDelay = 5 * 60 * 1000)
    public void poll() {
        try {
            this.executor.executeForScheduler(CrmSyncTaskRunner.SCHEDULER_NAME, this.runner, this::onJobState);
        } catch (Exception e) {
            this.logger.error(op("CRM-002") + "CRM sync job poll failed", e);
        }
    }

    private void onJobState(@NonNull Job job, @NonNull JobState state) {
        var status = switch (state) {
            case SUCCESS -> CrmSyncStatus.CRM_SYNC_STATUS_SUCCESS;
            case PARTIAL_SUCCESS -> CrmSyncStatus.CRM_SYNC_STATUS_PARTIAL;
            case FAILURE -> CrmSyncStatus.CRM_SYNC_STATUS_FAILED;
            // PENDING means tasks remain (the chain spawns them one tick at a time), RUNNING means this tick
            // just claimed the job. Neither is terminal, so the run stays as it is.
            case PENDING, RUNNING -> null;
        };
        if (status == null) {
            return;
        }

        // The run is found by job id rather than carried in: the listener only receives the job.
        this.runRepository.findByJobId(job.getId()).ifPresent(run ->
                this.runUpdater.finish(
                        new CrmSyncRunRef(run.getCustomerId(), run.getProjectId(), run.getId()), status, null));
    }
}
