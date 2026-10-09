package com.bulldozer_collective.modules.crm.stages.services;

import com.bulldozer_collective.modules.crm.api.CrmStageSource;
import com.bulldozer_collective.modules.crm.stages.data.CrmStageSourceEntity;
import com.bulldozer_collective.shared.ModelConverter;
import org.springframework.stereotype.Service;

@Service
public class CrmStageSourceConverter implements ModelConverter<CrmStageSourceEntity, CrmStageSource> {

    @Override
    public CrmStageSource convert(CrmStageSourceEntity e) {
        return CrmStageSource.builder()
                .pipelineId(e.getPipelineId())
                .propertyName(e.getPropertyName())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
