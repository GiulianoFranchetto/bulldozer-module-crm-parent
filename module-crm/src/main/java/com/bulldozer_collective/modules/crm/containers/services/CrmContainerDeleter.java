package com.bulldozer_collective.modules.crm.containers.services;

import com.bulldozer_collective.modules.crm.containers.data.CrmContainerRepository;
import com.bulldozer_collective.modules.crm.containers.data.CrmContainerStageRepository;
import com.bulldozer_collective.logging.BulldozerLogger;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static com.bulldozer_collective.modules.crm.configurations.CrmLogContext.op;

@Service
@RequiredArgsConstructor
public class CrmContainerDeleter {

    private final CrmContainerFetcher fetcher;
    private final CrmContainerRepository containerRepository;
    private final CrmContainerStageRepository stageRepository;
    private final BulldozerLogger logger;

    /** Deletes the container and its stages. Nothing is touched in the CRM. */
    @Transactional
    public void delete(@NonNull UUID customerId,
                       @NonNull UUID projectId,
                       @NonNull UUID connectionId,
                       @NonNull UUID containerId) {
        var container = this.fetcher.fetch(customerId, projectId, connectionId, containerId);
        this.stageRepository.deleteByCustomerIdAndProjectIdAndContainer_Id(customerId, projectId, containerId);
        this.containerRepository.delete(container);
        this.logger.info(op("CRM-003", customerId, projectId) + "Deleted container %s", containerId);
    }
}
