package com.bulldozer_collective.modules.crm.pipelines.services;

import com.bulldozer_collective.modules.crm.api.CrmPipeline;
import com.bulldozer_collective.modules.crm.api.CrmPipelineStage;
import com.bulldozer_collective.modules.crm.pipelines.data.CrmDealStageMappingRepository;
import com.bulldozer_collective.modules.crm.pipelines.data.CrmPipelineEntity;
import com.bulldozer_collective.modules.crm.pipelines.data.CrmPipelineStageRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CrmPipelineConverter {

    private final CrmPipelineStageRepository stageRepository;
    private final CrmDealStageMappingRepository mappingRepository;

    public CrmPipeline convert(@NonNull CrmPipelineEntity entity) {
        var stages = this.stageRepository
                .findAllByPipelineIdAndDeletedAtIsNullOrderByDisplayOrderAsc(entity.getId())
                .stream()
                .map(stage -> {
                    var mapping = this.mappingRepository.findByPipelineStageIdAndCustomerIdAndProjectId(
                            stage.getId(), entity.getCustomerId(), entity.getProjectId());
                    return new CrmPipelineStage(
                            stage.getId(),
                            stage.getSourceId(),
                            stage.getLabel(),
                            stage.getDisplayOrder(),
                            mapping.map(m -> m.getInternalStage()).orElse(null),
                            mapping.map(m -> m.getId()).orElse(null)
                    );
                })
                .toList();

        return new CrmPipeline(
                entity.getId(),
                entity.getCustomerId(),
                entity.getProjectId(),
                entity.getPlatform(),
                entity.getSourceId(),
                entity.getName(),
                stages
        );
    }

    public List<CrmPipeline> convert(@NonNull List<CrmPipelineEntity> entities) {
        return entities.stream().map(this::convert).toList();
    }
}
