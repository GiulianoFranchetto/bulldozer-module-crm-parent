package com.bulldozer_collective.modules.crm.sync.services;

import com.bulldozer_collective.jobs.lib.IJobTaskRunner;
import com.bulldozer_collective.jobs.lib.JobNonRecoverableException;
import com.bulldozer_collective.jobs.lib.JobTask;
import com.bulldozer_collective.jobs.lib.JobTaskParam;
import com.bulldozer_collective.jobs.lib.JobTaskResult;
import com.bulldozer_collective.logging.BulldozerLogger;
import com.bulldozer_collective.modules.crm.api.CrmSyncStatus;
import tools.jackson.databind.json.JsonMapper;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.bulldozer_collective.modules.crm.configurations.CrmLogContext.op;

/**
 * Runs the four steps of a CRM sync as chained job tasks.
 *
 * <p>The chain — pipelines, objects, associations, then canonical linking — is expressed by each task spawning the next one
 * rather than by enqueueing all three upfront: {@code JobJdbcRepository.listPendingTasks} orders by
 * {@code created_at, type}, and tasks created in the same loop share a millisecond timestamp, so insertion
 * order guarantees nothing. Deals cannot be resolved before their pipeline stages exist, associations cannot
 * be linked before the objects do, and a contact's employer is only known once its associations are in, hence
 * the strict sequencing. A spawned task runs on the next poller tick, so a full sync spans four ticks —
 * irrelevant for a twice-daily job.
 *
 * <p>Every failure is non-recoverable. The library applies no attempt cap, so rescheduling a task that fails
 * against a persistently broken portal would loop forever; the scheduler enqueues a fresh run twice a day,
 * and that is the retry.
 */
@Service
@RequiredArgsConstructor
public class CrmSyncTaskRunner implements IJobTaskRunner {

    /** Also the job-library scheduler name: the runner owns both halves of the contract. */
    public static final String SCHEDULER_NAME = "crm-sync";

    public static final String SYNC_PIPELINES = "SYNC_PIPELINES";
    public static final String SYNC_OBJECTS = "SYNC_OBJECTS";
    public static final String SYNC_ASSOCIATIONS = "SYNC_ASSOCIATIONS";
    public static final String LINK_CANONICAL = "LINK_CANONICAL";

    private final JsonMapper jsonMapper;
    private final CrmSyncPlatformResolver platformResolver;
    private final PlatformTokenFetcher tokenFetcher;
    private final CrmSyncValidator validator;
    private final CrmSyncRunUpdater runUpdater;
    private final CrmCanonicalLinker linker;
    private final BulldozerLogger logger;

    @Override
    public JobTaskResult run(@NonNull JobTask task) throws Throwable {
        var payload = this.readPayload(task);

        this.logger.debug(op("CRM-002", payload.customerId(), payload.projectId())
                + "Running CRM sync task %s on platform %s", task.getType(), payload.platform());

        try {
            return switch (task.getType()) {
                case SYNC_PIPELINES -> {
                    this.runPipelines(payload);
                    yield JobTaskResult.ok(List.of(JobTaskParam.critical(SYNC_OBJECTS, task.getPayload())));
                }
                case SYNC_OBJECTS -> {
                    this.runObjects(payload);
                    yield JobTaskResult.ok(List.of(JobTaskParam.critical(SYNC_ASSOCIATIONS, task.getPayload())));
                }
                case SYNC_ASSOCIATIONS -> {
                    this.runAssociations(payload);
                    // Non-critical: the mirrored CRM data is already correct and readable at this point, so a
                    // failure to reach the contacts / companies module must degrade the job to
                    // PARTIAL_SUCCESS, not discard the sync.
                    yield JobTaskResult.ok(List.of(JobTaskParam.nonCritical(LINK_CANONICAL, task.getPayload())));
                }
                case LINK_CANONICAL -> {
                    var progress = this.linker.linkBatch(
                            payload.customerId(), payload.projectId(), payload.platform());
                    // Rescheduling keeps each tick short and resumable. Guarded on actual progress: rows left
                    // but none linked means the remaining ones cannot be resolved right now, and the library
                    // applies no attempt cap — an unguarded reschedule would loop for ever.
                    yield progress.shouldContinue() ? JobTaskResult.reschedule() : JobTaskResult.ok();
                }
                default -> throw new JobNonRecoverableException("Unknown CRM sync task type: " + task.getType());
            };
        } catch (Throwable t) {
            // The job state listener writes the terminal status, but only the failing task knows why: record
            // the reason here so it is not lost.
            var reason = t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName();
            this.runUpdater.finish(CrmSyncRunRef.of(payload), CrmSyncStatus.CRM_SYNC_STATUS_FAILED,
                    "%s: %s".formatted(task.getType(), reason));
            throw t;
        }
    }

    private void runPipelines(@NonNull CrmSyncPayload payload) throws NoTokenException, CrmSyncValidationException {
        var token = this.tokenFetcher.fetchToken(payload.customerId(), payload.projectId(), payload.platform());
        this.platformResolver.resolve(payload.platform())
                .discoverPipelines(payload.customerId(), payload.projectId(), token);
        // Unmapped stages are a user-fixable condition, not a defect. Validating here keeps the sync from
        // silently dropping every deal whose stage has no mapping.
        this.validator.validate(payload.customerId(), payload.projectId(), payload.platform());
    }

    private void runObjects(@NonNull CrmSyncPayload payload) throws NoTokenException {
        var token = this.tokenFetcher.fetchToken(payload.customerId(), payload.projectId(), payload.platform());
        var result = this.platformResolver.resolve(payload.platform())
                .syncObjects(payload.customerId(), payload.projectId(), token);
        this.runUpdater.recordCounts(CrmSyncRunRef.of(payload), result);
    }

    private void runAssociations(@NonNull CrmSyncPayload payload) throws NoTokenException {
        var token = this.tokenFetcher.fetchToken(payload.customerId(), payload.projectId(), payload.platform());
        this.platformResolver.resolve(payload.platform())
                .syncAssociations(payload.customerId(), payload.projectId(), token);
    }

    private CrmSyncPayload readPayload(@NonNull JobTask task) throws JobNonRecoverableException {
        var raw = task.getPayload();
        if (raw == null) {
            throw new JobNonRecoverableException("CRM sync task %s has no payload".formatted(task.getId()));
        }
        try {
            return this.jsonMapper.readValue(raw, CrmSyncPayload.class);
        } catch (Exception e) {
            throw new JobNonRecoverableException(e);
        }
    }
}
