package com.bulldozer_collective.modules.crm.companies.services;

import com.bulldozer_collective.modules.crm.api.CrmCompanyRecord;
import com.bulldozer_collective.modules.crm.companies.data.CrmCompanyEntity;
import com.bulldozer_collective.shared.ModelConverter;
import org.springframework.stereotype.Service;

@Service
public class CrmCompanyConverter implements ModelConverter<CrmCompanyEntity, CrmCompanyRecord> {

    @Override
    public CrmCompanyRecord convert(CrmCompanyEntity entity) {
        return new CrmCompanyRecord(
                entity.getId(),
                entity.getCustomerId(),
                entity.getProjectId(),
                entity.getPlatform(),
                entity.getSourceId(),
                entity.getName(),
                entity.getDomain(),
                entity.getIndustry(),
                entity.getCountry(),
                entity.getCity(),
                entity.getCreatedAt()
        );
    }
}
