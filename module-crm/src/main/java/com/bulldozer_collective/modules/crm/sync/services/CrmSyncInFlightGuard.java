package com.bulldozer_collective.modules.crm.sync.services;

import com.bulldozer_collective.jobs.lib.JobJdbcRepository;
import com.bulldozer_collective.jobs.lib.JobState;
import com.bulldozer_collective.logging.BulldozerLogger;
import com.bulldozer_collective.modules.crm.api.CrmSyncStatus;
import com.bulldozer_collective.modules.crm.sync.data.CrmSyncRunRepository;
import com.bulldozer_collective.shared.IntegrationPlatform;
import com.bulldozer_collective.shared.exceptions.ConflictException;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static com.bulldozer_collective.modules.crm.configurations.CrmLogContext.op;

/**
 * Refuses a second sync of a portal while one is still in flight. Without this, N calls to the trigger endpoint
 * create N jobs that re-fetch the same portal, burn its rate limit and write the same rows concurrently.
 *
 * <p>The decision is taken from the <b>job</b> state, not from the run's own status: a service killed mid-sync
 * leaves the run at RUNNING with no job left to finish it, and keying off the status alone would then lock the
 * portal out until the sweeper closes it. Mirrors {@code LinkedInPostSearchJobFactory.start} in outbound.
 */
@Service
@RequiredArgsConstructor
public class CrmSyncInFlightGuard {

    private final CrmSyncRunRepository runRepository;
    private final JobJdbcRepository jobRepository;
    private final BulldozerLogger logger;

    public void assertNoSyncInFlight(@NonNull UUID customerId,
                                    @NonNull UUID projectId,
                                    @NonNull IntegrationPlatform platform) {
        var running = this.runRepository
                .findAllByCustomerIdAndProjectIdAndPlatformAndStatus(
                        customerId, projectId, platform, CrmSyncStatus.CRM_SYNC_STATUS_RUNNING);

        for (var run : running) {
            var jobId = run.getJobId();
            if (jobId == null) {
                continue;
            }
            if (this.isJobLive(jobId)) {
                this.logger.warn(op("CRM-001", customerId, projectId)
                        + "Refusing a new sync on platform %s: run %s is still in flight", platform, run.getId());
                throw new ConflictException(
                        "A sync of %s is already in progress for this project (run %s)"
                                .formatted(platform.getNiceName(), run.getId()));
            }
        }
    }

    private boolean isJobLive(@NonNull UUID jobId) {
        try {
            var report = this.jobRepository.getReport(jobId);
            return report.state() == JobState.PENDING || report.state() == JobState.RUNNING;
        } catch (RuntimeException e) {
            // The job cleaner purges old rows, so an unknown job id means the job is long gone — not in flight.
            return false;
        }
    }
}
