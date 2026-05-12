package com.bulldozer_collective.modules.crm.pipelines.services;

import com.bulldozer_collective.modules.crm.api.CrmPipelineRecord;
import com.bulldozer_collective.modules.crm.api.CrmPipelineStageRecord;
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

    public CrmPipelineRecord convert(@NonNull CrmPipelineEntity entity) {
        var stages = this.stageRepository
                .findAllByPipelineIdOrderByDisplayOrderAsc(entity.getId())
                .stream()
                .map(stage -> {
                    var mapping = this.mappingRepository.findByPipelineStageId(stage.getId());
                    return new CrmPipelineStageRecord(
                            stage.getId(),
                            stage.getSourceId(),
                            stage.getLabel(),
                            stage.getDisplayOrder(),
                            mapping.map(m -> m.getInternalStage()).orElse(null),
                            mapping.map(m -> m.getId()).orElse(null)
                    );
                })
                .toList();

        return new CrmPipelineRecord(
                entity.getId(),
                entity.getCustomerId(),
                entity.getProjectId(),
                entity.getPlatform(),
                entity.getSourceId(),
                entity.getName(),
                stages
        );
    }

    public List<CrmPipelineRecord> convert(@NonNull List<CrmPipelineEntity> entities) {
        return entities.stream().map(this::convert).toList();
    }
}
