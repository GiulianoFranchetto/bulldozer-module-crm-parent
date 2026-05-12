package com.bulldozer_collective.modules.crm.contacts.services;

import com.bulldozer_collective.modules.crm.api.CrmContactRecord;
import com.bulldozer_collective.modules.crm.contacts.data.CrmContactEntity;
import com.bulldozer_collective.shared.ModelConverter;
import org.springframework.stereotype.Service;

@Service
public class CrmContactConverter implements ModelConverter<CrmContactEntity, CrmContactRecord> {

    @Override
    public CrmContactRecord convert(CrmContactEntity entity) {
        return new CrmContactRecord(
                entity.getId(),
                entity.getCustomerId(),
                entity.getProjectId(),
                entity.getPlatform(),
                entity.getSourceId(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getEmail(),
                entity.getPhone(),
                entity.getJobTitle(),
                entity.getLinkedInUrl(),
                entity.getCreatedAt()
        );
    }
}
