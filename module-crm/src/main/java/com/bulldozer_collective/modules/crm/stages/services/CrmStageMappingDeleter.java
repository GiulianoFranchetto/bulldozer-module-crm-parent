package com.bulldozer_collective.modules.crm.stages.services;

import com.bulldozer_collective.modules.crm.stages.data.CrmStageMappingRepository;
import com.bulldozer_collective.logging.BulldozerLogger;
import com.bulldozer_collective.shared.exceptions.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static com.bulldozer_collective.modules.crm.configurations.CrmLogContext.op;

@Service
@RequiredArgsConstructor
public class CrmStageMappingDeleter {

    private final CrmStageMappingRepository repository;
    private final BulldozerLogger logger;

    public void delete(@NonNull UUID customerId,
                       @NonNull UUID projectId,
                       @NonNull UUID connectionId,
                       @NonNull String pipelineId,
                       @NonNull UUID mappingId) {
        var mapping = this.repository
                .findByCustomerIdAndProjectIdAndConnectionIdAndPipelineIdAndId(
                        customerId, projectId, connectionId, pipelineId, mappingId)
                .orElseThrow(() -> new NotFoundException("No stage mapping " + mappingId + " in pipeline " + pipelineId));
        this.repository.delete(mapping);
        this.logger.info(op("CRM-003", customerId, projectId) + "Deleted stage mapping %s of pipeline %s",
                mappingId, pipelineId);
    }
}
