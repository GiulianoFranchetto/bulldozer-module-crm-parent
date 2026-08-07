package com.bulldozer_collective.modules.crm.sync.services;

import com.bulldozer_collective.shared.IntegrationPlatform;
import com.bulldozer_collective.modules.crm.api.CrmSyncStatus;
import com.bulldozer_collective.modules.crm.sync.data.CrmSyncRunEntity;
import com.bulldozer_collective.modules.crm.sync.data.CrmSyncRunRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CrmSyncRunFetcher {

    private final CrmSyncRunRepository repository;

    public Page<CrmSyncRunEntity> search(@NonNull ListCrmSyncRunsQuery query) {
        return this.repository.search(query.customerId(), query.projectId(), query.platform(), query.status(),
                PageRequest.of(query.page(), query.size()));
    }
}
