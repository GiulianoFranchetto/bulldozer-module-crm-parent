package com.bulldozer_collective.modules.crm.containers.services;

import com.bulldozer_collective.modules.crm.api.CrmContainerStage;
import com.bulldozer_collective.modules.crm.containers.data.CrmContainerStageEntity;
import com.bulldozer_collective.shared.ModelConverter;
import org.springframework.stereotype.Service;

@Service
public class CrmContainerStageConverter implements ModelConverter<CrmContainerStageEntity, CrmContainerStage> {

    @Override
    public CrmContainerStage convert(CrmContainerStageEntity e) {
        return CrmContainerStage.builder()
                .id(e.getId())
                .remoteId(e.getRemoteId())
                .name(e.getName())
                .displayOrder(e.getDisplayOrder())
                .dealStage(e.getDealStage())
                .leadStage(e.getLeadStage())
                .writeDefault(e.isWriteDefault())
                .build();
    }
}
