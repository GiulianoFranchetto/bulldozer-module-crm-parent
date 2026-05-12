package com.bulldozer_collective.modules.crm.pipelines.services;

import com.bulldozer_collective.modules.crm.pipelines.data.CrmDealStageMappingRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CrmStageMappingDeleter {

    private final CrmDealStageMappingRepository mappingRepository;

    @Transactional
    public void delete(@NonNull UUID mappingId,
                       @NonNull UUID customerId,
                       @NonNull UUID projectId) {
        var mapping = this.mappingRepository.findById(mappingId)
                .filter(m -> m.getCustomerId().equals(customerId) && m.getProjectId().equals(projectId))
                .orElseThrow(() -> new IllegalArgumentException("Mapping not found: " + mappingId));
        this.mappingRepository.delete(mapping);
    }
}
