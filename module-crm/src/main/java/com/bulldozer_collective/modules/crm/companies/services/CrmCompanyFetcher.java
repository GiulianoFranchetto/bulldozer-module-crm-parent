package com.bulldozer_collective.modules.crm.companies.services;

import com.bulldozer_collective.shared.IntegrationPlatform;
import com.bulldozer_collective.modules.crm.companies.data.CrmCompanyEntity;
import com.bulldozer_collective.modules.crm.companies.data.CrmCompanyRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CrmCompanyFetcher {

    private final CrmCompanyRepository repository;

    public Page<CrmCompanyEntity> search(@NonNull ListCrmCompaniesQuery query) {
        return this.repository.search(query.customerId(), query.projectId(), query.platform(), query.q(),
                PageRequest.of(query.page(), query.size()));
    }

    public Optional<CrmCompanyEntity> findById(@NonNull UUID id,
                                               @NonNull UUID customerId,
                                               @NonNull UUID projectId) {
        return this.repository.findByIdAndCustomerIdAndProjectIdAndDeletedAtIsNull(id, customerId, projectId);
    }
}
