package com.bulldozer_collective.modules.crm.sync.services;

import com.bulldozer_collective.shared.IntegrationPlatform;
import com.bulldozer_collective.modules.crm.api.CrmSyncStatus;
import com.bulldozer_collective.modules.crm.sync.data.CrmSyncRunEntity;
import com.bulldozer_collective.modules.crm.sync.data.CrmSyncRunRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CrmSyncRunFactory {

    private final CrmSyncRunRepository repository;

    public CrmSyncRunEntity create(@NonNull UUID customerId,
                                   @NonNull UUID projectId,
                                   @NonNull IntegrationPlatform platform) {
        var entity = new CrmSyncRunEntity();
        entity.setCustomerId(customerId);
        entity.setProjectId(projectId);
        entity.setPlatform(platform);
        entity.setStartedAt(Instant.now());
        entity.setStatus(CrmSyncStatus.RUNNING);
        entity.setContactsSynced(0);
        entity.setCompaniesSynced(0);
        entity.setDealsSynced(0);
        entity.setActivitiesSynced(0);
        return this.repository.save(entity);
    }
}
