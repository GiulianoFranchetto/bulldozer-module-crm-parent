package com.bulldozer_collective.modules.crm.sync.services;

import com.bulldozer_collective.shared.IntegrationPlatform;
import com.bulldozer_collective.modules.crm.api.CrmSyncStatus;
import com.bulldozer_collective.modules.crm.sync.data.CrmSyncRunEntity;
import com.bulldozer_collective.modules.crm.sync.data.CrmSyncRunRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CrmSyncRunFetcher {

    private final CrmSyncRunRepository repository;

    public Page<CrmSyncRunEntity> search(@NonNull UUID customerId,
                                         @NonNull UUID projectId,
                                         @Nullable IntegrationPlatform platform,
                                         @Nullable CrmSyncStatus status,
                                         int page,
                                         int size) {
        return this.repository.search(customerId, projectId, platform, status, PageRequest.of(page, size));
    }
}
