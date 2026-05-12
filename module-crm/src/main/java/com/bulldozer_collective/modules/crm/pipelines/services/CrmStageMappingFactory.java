package com.bulldozer_collective.modules.crm.pipelines.services;

import com.bulldozer_collective.modules.crm.api.CrmDealStage;
import com.bulldozer_collective.modules.crm.pipelines.data.CrmDealStageMappingEntity;
import com.bulldozer_collective.modules.crm.pipelines.data.CrmDealStageMappingRepository;
import com.bulldozer_collective.modules.crm.pipelines.data.CrmPipelineStageRepository;
import com.bulldozer_collective.shared.exceptions.ConflictException;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CrmStageMappingFactory {

    private final CrmPipelineStageRepository stageRepository;
    private final CrmDealStageMappingRepository mappingRepository;

    public CrmDealStageMappingEntity create(@NonNull UUID customerId,
                                            @NonNull UUID projectId,
                                            @NonNull UUID pipelineStageId,
                                            @NonNull CrmDealStage internalStage) {
        this.stageRepository.findById(pipelineStageId)
                .orElseThrow(() -> new IllegalArgumentException("Pipeline stage not found: " + pipelineStageId));

        this.mappingRepository.findByPipelineStageId(pipelineStageId)
                .ifPresent(existing -> {
                    throw new ConflictException();
                });

        var entity = new CrmDealStageMappingEntity();
        entity.setCustomerId(customerId);
        entity.setProjectId(projectId);
        entity.setPipelineStageId(pipelineStageId);
        entity.setInternalStage(internalStage);
        return this.mappingRepository.save(entity);
    }
}
