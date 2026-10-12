package com.bulldozer_collective.modules.crm.containers.services;

import com.bulldozer_collective.modules.crm.gateway.CrmRemoteContainer;
import com.bulldozer_collective.modules.crm.connections.services.CrmConnectionFetcher;
import com.bulldozer_collective.modules.crm.containers.data.CrmContainerEntity;
import com.bulldozer_collective.modules.crm.gateway.CrmGatewayResolver;
import com.bulldozer_collective.modules.crm.gateway.CrmSessionOpener;
import com.bulldozer_collective.modules.crm.gateway.NoTokenException;
import com.bulldozer_collective.shared.exceptions.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CrmContainerFactory {

    private final CrmConnectionFetcher connectionFetcher;
    private final CrmSessionOpener sessionOpener;
    private final CrmGatewayResolver gatewayResolver;
    private final CrmStageSources stageSources;
    private final CrmContainerPersister persister;

    /**
     * Imports a CRM container, with the values of its stage source as stages, all unmapped. Without a stage
     * source, the CRM's native stage property is used.
     *
     * @throws NotFoundException        when the CRM has no such pipeline.
     * @throws IllegalArgumentException when the property does not exist or is not enumerated.
     */
    public @NonNull CrmContainerEntity create(@NonNull UUID customerId,
                                              @NonNull UUID projectId,
                                              @NonNull UUID connectionId,
                                              @NonNull ImportCrmContainerBuilder container) throws NoTokenException {
        var connection = this.connectionFetcher.fetch(customerId, projectId, connectionId);
        var session = this.sessionOpener.open(customerId, projectId, connectionId);
        var gateway = this.gatewayResolver.resolve(session);

        var remote = gateway.listRemoteContainers(session, container.type()).stream()
                .filter(candidate -> candidate.remoteId().equals(container.remoteId()))
                .findFirst()
                .map(CrmRemoteContainer::name)
                .orElseThrow(() -> new NotFoundException("The CRM has no pipeline " + container.remoteId()));
        var stageProperty = container.stagePropertyName() != null
                ? container.stagePropertyName()
                : gateway.nativeStageProperty(container.type());
        var options = this.stageSources.read(session, gateway, container.type(), container.remoteId(), stageProperty);

        return this.persister.create(connection, container.type(), container.remoteId(), remote, stageProperty,
                options);
    }
}
