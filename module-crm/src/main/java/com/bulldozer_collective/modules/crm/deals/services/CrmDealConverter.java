package com.bulldozer_collective.modules.crm.deals.services;

import com.bulldozer_collective.modules.crm.api.CrmDealRecord;
import com.bulldozer_collective.modules.crm.deals.data.CrmDealEntity;
import com.bulldozer_collective.shared.ModelConverter;
import org.springframework.stereotype.Service;

@Service
public class CrmDealConverter implements ModelConverter<CrmDealEntity, CrmDealRecord> {

    @Override
    public CrmDealRecord convert(CrmDealEntity entity) {
        return new CrmDealRecord(
                entity.getId(),
                entity.getCustomerId(),
                entity.getProjectId(),
                entity.getPlatform(),
                entity.getSourceId(),
                entity.getName(),
                entity.getAmount(),
                entity.getCurrency(),
                entity.getCloseDate(),
                entity.getInternalStage(),
                entity.getPipelineId(),
                entity.getPipelineStageId(),
                entity.getCreatedAt()
        );
    }
}
