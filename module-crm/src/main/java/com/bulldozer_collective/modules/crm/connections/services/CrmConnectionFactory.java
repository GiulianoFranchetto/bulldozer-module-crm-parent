package com.bulldozer_collective.modules.crm.connections.services;

import com.bulldozer_collective.shared.IntegrationPlatform;
import com.bulldozer_collective.modules.crm.connections.data.CrmConnectionEntity;
import com.bulldozer_collective.modules.crm.connections.data.CrmConnectionRepository;
import com.bulldozer_collective.shared.exceptions.ConflictException;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CrmConnectionFactory {

    private final CrmConnectionRepository repository;

    public CrmConnectionEntity create(@NonNull UUID customerId,
                                      @NonNull UUID projectId,
                                      @NonNull IntegrationPlatform platform) {
        this.repository.findByCustomerIdAndProjectIdAndPlatform(customerId, projectId, platform)
                .ifPresent(existing -> {
                    throw new ConflictException();
                });

        var entity = new CrmConnectionEntity();
        entity.setCustomerId(customerId);
        entity.setProjectId(projectId);
        entity.setPlatform(platform);
        return this.repository.save(entity);
    }
}
