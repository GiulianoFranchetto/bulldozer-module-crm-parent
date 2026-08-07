package com.bulldozer_collective.modules.crm.activities.services;

import com.bulldozer_collective.modules.crm.activities.data.CrmActivityEntity;
import com.bulldozer_collective.modules.crm.activities.data.CrmActivityRepository;
import com.bulldozer_collective.modules.crm.api.CrmActivityType;
import com.bulldozer_collective.shared.IntegrationPlatform;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CrmActivityFetcher {

    private final CrmActivityRepository repository;

    public Page<CrmActivityEntity> search(@NonNull ListCrmActivitiesQuery query) {
        return this.repository.search(query.customerId(), query.projectId(), query.platform(), query.type(), query.dealId(),
                query.contactId(), query.companyId(),
                PageRequest.of(query.page(), query.size()));
    }

    public Optional<CrmActivityEntity> findById(@NonNull UUID id,
                                                @NonNull UUID customerId,
                                                @NonNull UUID projectId) {
        return this.repository.findByIdAndCustomerIdAndProjectIdAndDeletedAtIsNull(id, customerId, projectId);
    }
}
