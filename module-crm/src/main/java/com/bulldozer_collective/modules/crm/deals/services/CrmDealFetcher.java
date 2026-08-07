package com.bulldozer_collective.modules.crm.deals.services;

import com.bulldozer_collective.modules.crm.api.CrmDealStage;
import com.bulldozer_collective.shared.IntegrationPlatform;
import com.bulldozer_collective.modules.crm.deals.data.CrmDealEntity;
import com.bulldozer_collective.modules.crm.deals.data.CrmDealRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CrmDealFetcher {

    private final CrmDealRepository repository;

    public Page<CrmDealEntity> search(@NonNull ListCrmDealsQuery query) {
        return this.repository.search(query.customerId(), query.projectId(), query.platform(), query.internalStage(), query.pipelineId(),
                PageRequest.of(query.page(), query.size()));
    }

    public Optional<CrmDealEntity> findById(@NonNull UUID id,
                                            @NonNull UUID customerId,
                                            @NonNull UUID projectId) {
        return this.repository.findByIdAndCustomerIdAndProjectIdAndDeletedAtIsNull(id, customerId, projectId);
    }
}
