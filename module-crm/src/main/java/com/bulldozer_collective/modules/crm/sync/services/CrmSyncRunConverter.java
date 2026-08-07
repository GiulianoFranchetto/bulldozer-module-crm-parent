package com.bulldozer_collective.modules.crm.sync.services;

import com.bulldozer_collective.modules.crm.api.CrmSyncRun;
import com.bulldozer_collective.modules.crm.sync.data.CrmSyncRunEntity;
import com.bulldozer_collective.shared.ModelConverter;
import org.springframework.stereotype.Service;

@Service
public class CrmSyncRunConverter implements ModelConverter<CrmSyncRunEntity, CrmSyncRun> {

    @Override
    public CrmSyncRun convert(CrmSyncRunEntity entity) {
        return new CrmSyncRun(
                entity.getId(),
                entity.getCustomerId(),
                entity.getProjectId(),
                entity.getPlatform(),
                entity.getStartedAt(),
                entity.getFinishedAt(),
                entity.getStatus(),
                entity.getContactsSynced(),
                entity.getCompaniesSynced(),
                entity.getDealsSynced(),
                entity.getActivitiesSynced(),
                entity.getErrorMessage()
        );
    }
}
