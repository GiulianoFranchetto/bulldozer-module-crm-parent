package com.bulldozer_collective.modules.crm.connections.services;

import com.bulldozer_collective.modules.crm.connections.data.CrmConnectionEntity;
import com.bulldozer_collective.modules.crm.connections.data.CrmConnectionRepository;
import com.bulldozer_collective.shared.exceptions.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CrmConnectionFetcher {

    private final CrmConnectionRepository repository;

    public @NonNull List<CrmConnectionEntity> fetchAll(@NonNull UUID customerId, @NonNull UUID projectId) {
        return this.repository.findAllByCustomerIdAndProjectIdOrderByCreatedAtAsc(customerId, projectId);
    }

    /** @throws NotFoundException when the connection does not exist in this project. */
    public @NonNull CrmConnectionEntity fetch(@NonNull UUID customerId,
                                              @NonNull UUID projectId,
                                              @NonNull UUID connectionId) {
        return this.repository.findByCustomerIdAndProjectIdAndId(customerId, projectId, connectionId)
                .orElseThrow(() -> new NotFoundException("No CRM connection " + connectionId + " in this project"));
    }
}
