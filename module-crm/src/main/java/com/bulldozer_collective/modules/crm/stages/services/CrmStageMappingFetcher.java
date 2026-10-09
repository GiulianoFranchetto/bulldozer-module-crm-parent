package com.bulldozer_collective.modules.crm.stages.services;

import com.bulldozer_collective.modules.crm.connections.services.CrmConnectionFetcher;
import com.bulldozer_collective.modules.crm.stages.data.CrmStageMappingEntity;
import com.bulldozer_collective.modules.crm.stages.data.CrmStageMappingRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CrmStageMappingFetcher {

    private final CrmConnectionFetcher connectionFetcher;
    private final CrmStageMappingRepository repository;

    public @NonNull List<CrmStageMappingEntity> fetchAll(@NonNull UUID customerId,
                                                         @NonNull UUID projectId,
                                                         @NonNull UUID connectionId,
                                                         @NonNull String pipelineId) {
        this.connectionFetcher.fetch(customerId, projectId, connectionId);
        return this.repository.findAllByCustomerIdAndProjectIdAndConnectionIdAndPipelineIdOrderByCreatedAtAsc(
                customerId, projectId, connectionId, pipelineId);
    }
}
