package com.bulldozer_collective.modules.crm.stages.services;

import com.bulldozer_collective.modules.crm.connections.services.CrmConnectionFetcher;
import com.bulldozer_collective.modules.crm.stages.data.CrmStageSourceEntity;
import com.bulldozer_collective.modules.crm.stages.data.CrmStageSourceRepository;
import com.bulldozer_collective.shared.exceptions.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CrmStageSourceFetcher {

    private final CrmConnectionFetcher connectionFetcher;
    private final CrmStageSourceRepository repository;

    /** @throws NotFoundException when the connection does not exist, or the pipeline has no stage source. */
    public @NonNull CrmStageSourceEntity fetch(@NonNull UUID customerId,
                                               @NonNull UUID projectId,
                                               @NonNull UUID connectionId,
                                               @NonNull String pipelineId) {
        this.connectionFetcher.fetch(customerId, projectId, connectionId);
        return this.repository
                .findByCustomerIdAndProjectIdAndConnectionIdAndPipelineId(customerId, projectId, connectionId, pipelineId)
                .orElseThrow(() -> new NotFoundException("Pipeline " + pipelineId + " has no stage source"));
    }
}
