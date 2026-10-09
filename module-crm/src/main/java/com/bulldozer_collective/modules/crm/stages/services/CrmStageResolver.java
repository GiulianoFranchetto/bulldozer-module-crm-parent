package com.bulldozer_collective.modules.crm.stages.services;

import com.bulldozer_collective.modules.crm.stages.data.CrmStageMappingRepository;
import com.bulldozer_collective.modules.crm.stages.data.CrmStageSourceRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CrmStageResolver {

    private final CrmStageSourceRepository sourceRepository;
    private final CrmStageMappingRepository mappingRepository;

    public @NonNull CrmStageTable load(@NonNull UUID customerId, @NonNull UUID projectId, @NonNull UUID connectionId) {
        return new CrmStageTable(
                this.sourceRepository.findAllByCustomerIdAndProjectIdAndConnectionId(customerId, projectId, connectionId),
                this.mappingRepository.findAllByCustomerIdAndProjectIdAndConnectionId(customerId, projectId, connectionId));
    }
}
