package com.bulldozer_collective.modules.crm.containers.services;

import com.bulldozer_collective.modules.crm.api.CrmContainerType;
import com.bulldozer_collective.modules.crm.connections.services.CrmConnectionFetcher;
import com.bulldozer_collective.modules.crm.containers.data.CrmContainerEntity;
import com.bulldozer_collective.modules.crm.gateway.CrmGatewayResolver;
import com.bulldozer_collective.modules.crm.gateway.CrmRemoteContainer;
import com.bulldozer_collective.modules.crm.gateway.CrmSessionOpener;
import com.bulldozer_collective.modules.crm.gateway.NoTokenException;
import com.bulldozer_collective.shared.exceptions.ConflictException;
import com.bulldozer_collective.shared.exceptions.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
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
     * Imports CRM containers, all or none, each with the values of its stage source as stages, all unmapped. Without
     * a stage source, the CRM's native stage property is used. Everything is read from the CRM first; the
     * containers are written in one transaction afterwards.
     *
     * @throws NotFoundException        when the CRM has no such container.
     * @throws ConflictException        when a container is already imported on that connection.
     * @throws IllegalArgumentException when a container is listed twice, or a property does not exist or is not
     *                                  enumerated.
     */
    public @NonNull List<CrmContainerEntity> create(@NonNull UUID customerId,
                                                    @NonNull UUID projectId,
                                                    @NonNull UUID connectionId,
                                                    @NonNull List<ImportCrmContainerBuilder> containers)
            throws NoTokenException {
        var seen = new HashSet<String>();
        for (var container : containers) {
            if (!seen.add(container.type() + "/" + container.remoteId())) {
                throw new IllegalArgumentException(
                        "containers: %s '%s' is listed twice".formatted(container.type(), container.remoteId()));
            }
        }

        var connection = this.connectionFetcher.fetch(customerId, projectId, connectionId);
        var session = this.sessionOpener.open(customerId, projectId, connectionId);
        var gateway = this.gatewayResolver.resolve(session);

        var remotes = new EnumMap<CrmContainerType, List<CrmRemoteContainer>>(CrmContainerType.class);
        var created = new ArrayList<CrmContainerPersister.NewContainer>();
        for (var container : containers) {
            var name = remotes.computeIfAbsent(container.type(), type -> gateway.listRemoteContainers(session, type))
                    .stream()
                    .filter(candidate -> candidate.remoteId().equals(container.remoteId()))
                    .findFirst()
                    .map(CrmRemoteContainer::name)
                    .orElseThrow(() -> new NotFoundException("The CRM has no container " + container.remoteId()));
            var stageProperty = container.stagePropertyName() != null
                    ? container.stagePropertyName()
                    : gateway.nativeStageProperty(container.type());
            var options = this.stageSources.read(session, gateway, container.type(), container.remoteId(),
                    stageProperty);
            created.add(new CrmContainerPersister.NewContainer(container.type(), container.remoteId(), name,
                    stageProperty, options));
        }

        try {
            return this.persister.create(connection, created);
        } catch (DataIntegrityViolationException e) {
            // Two concurrent imports passed the duplicate check: the unique constraint let one through.
            throw new ConflictException("Already imported: one of these containers was imported meanwhile");
        }
    }
}
