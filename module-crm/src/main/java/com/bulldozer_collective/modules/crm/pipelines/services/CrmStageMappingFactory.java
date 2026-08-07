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

    public CrmDealStageMappingEntity create(@NonNull CreateCrmStageMappingBuilder request) {
        var customerId = request.customerId();
        var projectId = request.projectId();
        var pipelineStageId = request.pipelineStageId();
        var internalStage = request.internalStage();

        // Tenant-scoped on purpose: an unscoped findById let a caller map a stage belonging to another
        // customer, and — since a stage carries at most one mapping — block that customer's own mapping.
        this.stageRepository.findByIdAndCustomerIdAndProjectIdAndDeletedAtIsNull(pipelineStageId, customerId, projectId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "pipelineStageId: '%s' is not a known pipeline stage of this project"
                                .formatted(pipelineStageId)));

        this.mappingRepository.findByPipelineStageIdAndCustomerIdAndProjectId(pipelineStageId, customerId, projectId)
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
