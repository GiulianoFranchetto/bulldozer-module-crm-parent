package com.bulldozer_collective.modules.crm.deals.services;

import com.bulldozer_collective.modules.crm.api.CrmDeal;
import com.bulldozer_collective.modules.crm.deals.data.CrmDealEntity;
import com.bulldozer_collective.shared.ModelConverter;
import org.springframework.stereotype.Service;

@Service
public class CrmDealConverter implements ModelConverter<CrmDealEntity, CrmDeal> {

    @Override
    public CrmDeal convert(CrmDealEntity entity) {
        return new CrmDeal(
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
