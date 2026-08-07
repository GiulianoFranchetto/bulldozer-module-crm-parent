package com.bulldozer_collective.modules.crm.sync.services;

import com.bulldozer_collective.modules.crm.api.CrmSyncStatus;
import com.bulldozer_collective.modules.crm.sync.data.CrmSyncRunEntity;
import com.bulldozer_collective.modules.crm.sync.data.CrmSyncRunRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

/**
 * Owns every write to a {@link CrmSyncRunEntity} after its creation. The sync itself runs outside any
 * transaction (it performs remote HTTP calls), so the run row is updated in these short, isolated writes.
 */
@Service
@RequiredArgsConstructor
public class CrmSyncRunUpdater {

    private final CrmSyncRunRepository repository;

    public void recordCounts(@NonNull CrmSyncRunRef ref, @NonNull CrmSyncResult result) {
        this.find(ref).ifPresent(run -> {
            run.setContactsSynced(result.getContactsSynced());
            run.setCompaniesSynced(result.getCompaniesSynced());
            run.setDealsSynced(result.getDealsSynced());
            run.setActivitiesSynced(result.getActivitiesSynced());
            this.repository.save(run);
        });
    }

    /**
     * Records the outcome of the run. {@code errorMessage} is only written when given, so the message set by
     * the failing task survives the terminal status written afterwards by the job state listener.
     */
    public Optional<CrmSyncRunEntity> finish(@NonNull CrmSyncRunRef ref,
                                             @NonNull CrmSyncStatus status,
                                             @Nullable String errorMessage) {
        return this.find(ref).map(run -> {
            run.setStatus(status);
            run.setFinishedAt(Instant.now());
            if (errorMessage != null) {
                run.setErrorMessage(errorMessage);
            }
            return this.repository.save(run);
        });
    }

    private Optional<CrmSyncRunEntity> find(@NonNull CrmSyncRunRef ref) {
        return this.repository.findByIdAndCustomerIdAndProjectId(
                ref.syncRunId(), ref.customerId(), ref.projectId());
    }
}
