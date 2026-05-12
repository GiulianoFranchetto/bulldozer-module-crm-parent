package com.bulldozer_collective.modules.crm.deals.services;

import com.bulldozer_collective.modules.crm.api.CrmDealStage;
import com.bulldozer_collective.shared.IntegrationPlatform;
import com.bulldozer_collective.modules.crm.deals.data.CrmDealEntity;
import com.bulldozer_collective.modules.crm.deals.data.CrmDealRepository;
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
public class CrmDealFetcher {

    private final CrmDealRepository repository;

    public Page<CrmDealEntity> search(@NonNull UUID customerId,
                                      @NonNull UUID projectId,
                                      @Nullable IntegrationPlatform platform,
                                      @Nullable CrmDealStage internalStage,
                                      @Nullable UUID pipelineId,
                                      int page,
                                      int size) {
        return this.repository.search(customerId, projectId, platform, internalStage, pipelineId, PageRequest.of(page, size));
    }

    public Optional<CrmDealEntity> findById(@NonNull UUID id,
                                            @NonNull UUID customerId,
                                            @NonNull UUID projectId) {
        return this.repository.findByIdAndCustomerIdAndProjectIdAndDeletedAtIsNull(id, customerId, projectId);
    }
}
