package com.bulldozer_collective.modules.crm.containers.services;

import com.bulldozer_collective.modules.crm.api.CrmContainer;
import com.bulldozer_collective.modules.crm.containers.data.CrmContainerEntity;
import com.bulldozer_collective.shared.ModelConverter;
import org.springframework.stereotype.Service;

@Service
public class CrmContainerConverter implements ModelConverter<CrmContainerEntity, CrmContainer> {

    @Override
    public CrmContainer convert(CrmContainerEntity e) {
        return CrmContainer.builder()
                .id(e.getId())
                .type(e.getType())
                .remoteId(e.getRemoteId())
                .name(e.getName())
                .stagePropertyName(e.getStagePropertyName())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
