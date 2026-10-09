package com.bulldozer_collective.modules.crm.stages.services;

import com.bulldozer_collective.modules.crm.api.CrmStageMapping;
import com.bulldozer_collective.modules.crm.stages.data.CrmStageMappingEntity;
import com.bulldozer_collective.shared.ModelConverter;
import org.springframework.stereotype.Service;

@Service
public class CrmStageMappingConverter implements ModelConverter<CrmStageMappingEntity, CrmStageMapping> {

    @Override
    public CrmStageMapping convert(CrmStageMappingEntity e) {
        return CrmStageMapping.builder()
                .id(e.getId())
                .pipelineId(e.getPipelineId())
                .value(e.getValue())
                .stage(e.getStage())
                .writeDefault(e.isWriteDefault())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
