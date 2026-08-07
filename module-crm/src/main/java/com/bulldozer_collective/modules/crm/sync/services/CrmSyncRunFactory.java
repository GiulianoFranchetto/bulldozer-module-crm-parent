package com.bulldozer_collective.modules.crm.sync.services;

import com.bulldozer_collective.modules.crm.api.CrmSyncStatus;
import com.bulldozer_collective.modules.crm.sync.data.CrmSyncRunEntity;
import com.bulldozer_collective.modules.crm.sync.data.CrmSyncRunRepository;
import com.bulldozer_collective.shared.IntegrationPlatform;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CrmSyncRunFactory {

    private final CrmSyncRunRepository repository;

    /**
     * Creates the run row already carrying its {@code jobId}. The caller must therefore create the job
     * <b>before</b> the run, and only create the job's first task afterwards: the poller runs on another
     * thread and looks the run up by job id, so a task that becomes claimable before the id is persisted can
     * complete against a run the listener cannot find — leaving it stuck at RUNNING until the sweeper.
     */
    public CrmSyncRunEntity create(@NonNull CreateCrmSyncRunBuilder request) {
        var entity = new CrmSyncRunEntity();
        entity.setCustomerId(request.customerId());
        entity.setProjectId(request.projectId());
        entity.setPlatform(request.platform());
        entity.setJobId(request.jobId());
        entity.setStartedAt(Instant.now());
        entity.setStatus(CrmSyncStatus.CRM_SYNC_STATUS_RUNNING);
        entity.setContactsSynced(0);
        entity.setCompaniesSynced(0);
        entity.setDealsSynced(0);
        entity.setActivitiesSynced(0);
        return this.repository.save(entity);
    }
}
