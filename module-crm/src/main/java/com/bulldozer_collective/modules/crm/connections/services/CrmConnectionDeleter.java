package com.bulldozer_collective.modules.crm.connections.services;

import com.bulldozer_collective.modules.crm.connections.data.CrmConnectionRepository;
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
public class CrmConnectionDeleter {

    private final CrmConnectionFetcher fetcher;
    private final CrmConnectionRepository connectionRepository;
    private final CrmContainerRepository containerRepository;
    private final CrmContainerStageRepository stageRepository;
    private final BulldozerLogger logger;

    /**
     * Deletes the connection with its containers and their stages, in one transaction. Nothing is touched in the CRM, and
     * the modulith keeps its token: disconnecting the OAuth app is done there.
     */
    @Transactional
    public void delete(@NonNull UUID customerId, @NonNull UUID projectId, @NonNull UUID connectionId) {
        this.fetcher.fetch(customerId, projectId, connectionId);
        this.stageRepository.deleteByCustomerIdAndProjectIdAndContainer_Connection_Id(customerId, projectId,
                connectionId);
        this.containerRepository.deleteByCustomerIdAndProjectIdAndConnection_Id(customerId, projectId, connectionId);
        this.connectionRepository.deleteByCustomerIdAndProjectIdAndId(customerId, projectId, connectionId);
        this.logger.info(op("CRM-001", customerId, projectId) + "Deleted connection %s", connectionId);
    }
}
