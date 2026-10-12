package com.bulldozer_collective.modules.crm.containers.services;

import com.bulldozer_collective.modules.crm.gateway.CrmRemoteContainer;
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
public class CrmContainerUpdater {

    private final CrmContainerFetcher fetcher;
    private final CrmSessionOpener sessionOpener;
    private final CrmGatewayResolver gatewayResolver;
    private final CrmStageSources stageSources;
    private final CrmContainerPersister persister;

    /**
     * Changes the property the container's stages are read from. Every stage and every mapping is replaced.
     *
     * @throws IllegalArgumentException when the property does not exist or is not enumerated.
     */
    public @NonNull CrmContainerEntity setStageSource(@NonNull UUID customerId,
                                                      @NonNull UUID projectId,
                                                      @NonNull UUID connectionId,
                                                      @NonNull UUID containerId,
                                                      @NonNull String propertyName) throws NoTokenException {
        var container = this.fetcher.fetch(customerId, projectId, connectionId, containerId);
        var session = this.sessionOpener.open(customerId, projectId, connectionId);
        var options = this.stageSources.read(session, this.gatewayResolver.resolve(session), container.getType(),
                container.getRemoteId(), propertyName);
        return this.persister.replaceStages(container, propertyName, options);
    }

    /**
     * Aligns the container on the CRM: its name, and its stages — new values added unmapped, vanished ones deleted
     * with their mapping.
     *
     * @throws NotFoundException when the CRM no longer has the pipeline.
     */
    public @NonNull CrmContainerEntity refresh(@NonNull UUID customerId,
                                               @NonNull UUID projectId,
                                               @NonNull UUID connectionId,
                                               @NonNull UUID containerId) throws NoTokenException {
        var container = this.fetcher.fetch(customerId, projectId, connectionId, containerId);
        var session = this.sessionOpener.open(customerId, projectId, connectionId);
        var gateway = this.gatewayResolver.resolve(session);

        var name = gateway.listRemoteContainers(session, container.getType()).stream()
                .filter(candidate -> candidate.remoteId().equals(container.getRemoteId()))
                .findFirst()
                .map(CrmRemoteContainer::name)
                .orElseThrow(() -> new NotFoundException(
                        "The CRM no longer has pipeline " + container.getRemoteId() + "; delete the container"));
        var options = this.stageSources.read(session, gateway, container.getType(), container.getRemoteId(),
                container.getStagePropertyName());
        return this.persister.refresh(container, name, options);
    }
}
