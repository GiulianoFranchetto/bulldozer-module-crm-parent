package com.bulldozer_collective.modules.crm.activities.services;

import com.bulldozer_collective.modules.crm.activities.data.CrmActivityEntity;
import com.bulldozer_collective.modules.crm.api.CrmActivityRecord;
import com.bulldozer_collective.shared.ModelConverter;
import org.springframework.stereotype.Service;

@Service
public class CrmActivityConverter implements ModelConverter<CrmActivityEntity, CrmActivityRecord> {

    @Override
    public CrmActivityRecord convert(CrmActivityEntity entity) {
        return new CrmActivityRecord(
                entity.getId(),
                entity.getCustomerId(),
                entity.getProjectId(),
                entity.getPlatform(),
                entity.getSourceId(),
                entity.getType(),
                entity.getTitle(),
                entity.getBody(),
                entity.getDueDate(),
                entity.getCompletedAt(),
                entity.getDealId(),
                entity.getContactId(),
                entity.getCompanyId(),
                entity.getCreatedAt()
        );
    }
}
