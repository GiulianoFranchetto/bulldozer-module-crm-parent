package com.bulldozer_collective.modules.crm.contacts.services;

import com.bulldozer_collective.shared.IntegrationPlatform;
import com.bulldozer_collective.modules.crm.contacts.data.CrmContactEntity;
import com.bulldozer_collective.modules.crm.contacts.data.CrmContactRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CrmContactFetcher {

    private final CrmContactRepository repository;

    public Page<CrmContactEntity> search(@NonNull UUID customerId,
                                         @NonNull UUID projectId,
                                         @Nullable IntegrationPlatform platform,
                                         @Nullable String q,
                                         int page,
                                         int size) {
        return this.repository.search(customerId, projectId, platform, q, PageRequest.of(page, size));
    }

    public Optional<CrmContactEntity> findById(@NonNull UUID id,
                                               @NonNull UUID customerId,
                                               @NonNull UUID projectId) {
        return this.repository.findByIdAndCustomerIdAndProjectIdAndDeletedAtIsNull(id, customerId, projectId);
    }
}
