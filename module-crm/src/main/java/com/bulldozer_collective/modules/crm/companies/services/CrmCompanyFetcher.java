package com.bulldozer_collective.modules.crm.companies.services;

import com.bulldozer_collective.shared.IntegrationPlatform;
import com.bulldozer_collective.modules.crm.companies.data.CrmCompanyEntity;
import com.bulldozer_collective.modules.crm.companies.data.CrmCompanyRepository;
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
public class CrmCompanyFetcher {

    private final CrmCompanyRepository repository;

    public Page<CrmCompanyEntity> search(@NonNull UUID customerId,
                                         @NonNull UUID projectId,
                                         @Nullable IntegrationPlatform platform,
                                         @Nullable String q,
                                         int page,
                                         int size) {
        return this.repository.search(customerId, projectId, platform, q, PageRequest.of(page, size));
    }

    public Optional<CrmCompanyEntity> findById(@NonNull UUID id,
                                               @NonNull UUID customerId,
                                               @NonNull UUID projectId) {
        return this.repository.findByIdAndCustomerIdAndProjectIdAndDeletedAtIsNull(id, customerId, projectId);
    }
}
