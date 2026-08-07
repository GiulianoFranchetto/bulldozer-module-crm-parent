package com.bulldozer_collective.modules.crm.companies.services;

import com.bulldozer_collective.modules.crm.api.CrmCompany;
import com.bulldozer_collective.modules.crm.companies.data.CrmCompanyEntity;
import com.bulldozer_collective.shared.ModelConverter;
import org.springframework.stereotype.Service;

@Service
public class CrmCompanyConverter implements ModelConverter<CrmCompanyEntity, CrmCompany> {

    @Override
    public CrmCompany convert(CrmCompanyEntity entity) {
        return new CrmCompany(
                entity.getId(),
                entity.getCustomerId(),
                entity.getProjectId(),
                entity.getPlatform(),
                entity.getSourceId(),
                entity.getCompanyId(),
                entity.getName(),
                entity.getDomain(),
                entity.getIndustry(),
                entity.getCountry(),
                entity.getCity(),
                entity.getCreatedAt()
        );
    }
}
