package com.bulldozer_collective.modules.crm.connections.services;

import com.bulldozer_collective.modules.crm.api.CrmConnection;
import com.bulldozer_collective.modules.crm.connections.data.CrmConnectionEntity;
import com.bulldozer_collective.shared.ModelConverter;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

@Service
public class CrmConnectionConverter implements ModelConverter<CrmConnectionEntity, CrmConnection> {

    @Override
    public @NonNull CrmConnection convert(@NonNull CrmConnectionEntity e) {
        return CrmConnection.builder()
                .id(e.getId())
                .platform(e.getPlatform())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
