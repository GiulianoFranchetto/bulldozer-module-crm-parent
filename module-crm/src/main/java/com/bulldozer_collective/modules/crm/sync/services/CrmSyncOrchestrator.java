package com.bulldozer_collective.modules.crm.sync.services;

import com.bulldozer_collective.jobs.lib.Job;
import com.bulldozer_collective.jobs.lib.JobJdbcRepository;
import com.bulldozer_collective.jobs.lib.JobTaskParam;
import com.bulldozer_collective.logging.BulldozerLogger;
import com.bulldozer_collective.modules.crm.api.CrmSyncStatus;
import com.bulldozer_collective.modules.crm.sync.data.CrmSyncRunEntity;
import com.bulldozer_collective.shared.IntegrationPlatform;
import com.bulldozer_collective.shared.exceptions.InternalException;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

import static com.bulldozer_collective.modules.crm.configurations.CrmLogContext.op;

/**
 * Enqueues a CRM sync. It does no remote I/O and holds no transaction: it writes the run row, creates a
 * job-library job with its first task, and returns immediately. The job poller picks it up and
 * {@link CrmSyncTaskRunner} carries out the work.
 *
 * <p>The write order — job, then run carrying its {@code jobId}, then the first task — is deliberate. The task
 * is what makes the job claimable by the poller, and the poller resolves the run <i>by job id</i>; creating the
 * task before the id is persisted let a fast-failing job complete against a run the listener could not find,
 * stranding it at RUNNING until the sweeper.
 */
@Service
@RequiredArgsConstructor
public class CrmSyncOrchestrator {

    private final CrmSyncRunFactory syncRunFactory;
    private final CrmSyncRunUpdater runUpdater;
    private final CrmSyncPlatformResolver platformResolver;
    private final CrmSyncInFlightGuard inFlightGuard;
    private final JobJdbcRepository jobRepository;
    private final JsonMapper jsonMapper;
    private final BulldozerLogger logger;

    public CrmSyncRunEntity enqueue(@NonNull UUID customerId,
                                    @NonNull UUID projectId,
                                    @NonNull IntegrationPlatform platform) {
        // Rejects a non-CRM platform (400) then a concurrent sync (409), before anything is written.
        this.platformResolver.resolve(platform);
        this.inFlightGuard.assertNoSyncInFlight(customerId, projectId, platform);

        var job = this.createJob(customerId, projectId, platform);
        var run = this.syncRunFactory.create(CreateCrmSyncRunBuilder.builder()
                .customerId(customerId)
                .projectId(projectId)
                .platform(platform)
                .jobId(job.getId())
                .build());

        try {
            var payload = this.jsonMapper.writeValueAsString(
                    new CrmSyncPayload(customerId, projectId, platform, run.getId()));
            this.jobRepository.createTask(job, JobTaskParam.critical(CrmSyncTaskRunner.SYNC_PIPELINES, payload));

            this.logger.info(op("CRM-001", customerId, projectId)
                            + "Enqueued CRM sync job %s for run %s on platform %s",
                    job.getId(), run.getId(), platform);
            return run;
        } catch (Exception e) {
            // The job exists but has no task, so nothing will ever run it: close the run now instead of leaving
            // it at RUNNING until the sweeper times it out six hours later.
            this.logger.error(op("CRM-001", customerId, projectId)
                    + "Could not enqueue CRM sync job for run %s on platform %s", run.getId(), platform, e);
            return this.runUpdater.finish(
                    new CrmSyncRunRef(customerId, projectId, run.getId()),
                    CrmSyncStatus.CRM_SYNC_STATUS_FAILED,
                    "Could not enqueue the sync job: " + e.getMessage()).orElse(run);
        }
    }

    private Job createJob(@NonNull UUID customerId,
                          @NonNull UUID projectId,
                          @NonNull IntegrationPlatform platform) {
        try {
            return this.jobRepository.createNew(CrmSyncTaskRunner.SCHEDULER_NAME);
        } catch (Exception e) {
            // No run row is written in this case: there is nothing to report a failure on, and the cause is our
            // own database rather than anything the caller sent.
            this.logger.error(op("CRM-001", customerId, projectId)
                    + "Could not create the sync job on platform %s", platform, e);
            throw new InternalException("Could not create the CRM sync job", e);
        }
    }
}
