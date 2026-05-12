package com.bulldozer_collective.modules.crm.connections.services;

import com.bulldozer_collective.modules.crm.connections.data.CrmConnectionEntity;
import com.bulldozer_collective.modules.crm.connections.data.CrmConnectionRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CrmConnectionFetcher {

    private final CrmConnectionRepository repository;

    public List<CrmConnectionEntity> fetchAll(@NonNull UUID customerId, @NonNull UUID projectId) {
        return this.repository.findAllByCustomerIdAndProjectId(customerId, projectId);
    }

    public List<CrmConnectionEntity> fetchAllActive() {
        return this.repository.findAllByDeletedAtIsNull();
    }
}
