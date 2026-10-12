package com.bulldozer_collective.modules.crm.containers.services;

import com.bulldozer_collective.modules.crm.api.CrmContainerEntry;
import com.bulldozer_collective.modules.crm.api.CrmContainerType;
import com.bulldozer_collective.modules.crm.api.CrmProperty;
import com.bulldozer_collective.modules.crm.containers.data.CrmContainerEntity;
import com.bulldozer_collective.modules.crm.gateway.CrmGatewayResolver;
import com.bulldozer_collective.modules.crm.gateway.CrmRemoteContainer;
import com.bulldozer_collective.modules.crm.gateway.CrmSessionOpener;
import com.bulldozer_collective.modules.crm.gateway.NoTokenException;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

/** What the project chooses its containers from: the CRM's containers and properties, read live. */
@Service
@RequiredArgsConstructor
public class CrmContainerSearcher {

    private final CrmContainerFetcher fetcher;
    private final CrmSessionOpener sessionOpener;
    private final CrmGatewayResolver gatewayResolver;

    /**
     * Every container of the CRM, flagged {@code selected} with its {@code containerId} when the project imported
     * it, in the CRM's order; then the imported containers the CRM no longer has.
     *
     * @param type {@code null} lists the containers of deals and of leads.
     */
    public @NonNull List<CrmContainerEntry> list(@NonNull UUID customerId,
                                                 @NonNull UUID projectId,
                                                 @NonNull UUID connectionId,
                                                 @Nullable CrmContainerType type) throws NoTokenException {
        var imported = this.fetcher.fetchAll(customerId, projectId, connectionId, type);
        var session = this.sessionOpener.open(customerId, projectId, connectionId);
        var gateway = this.gatewayResolver.resolve(session);

        var entries = new ArrayList<CrmContainerEntry>();
        var matched = new HashSet<UUID>();
        var types = type == null ? Arrays.asList(CrmContainerType.values()) : List.of(type);
        for (var current : types) {
            for (var remote : gateway.listRemoteContainers(session, current)) {
                var container = find(imported, current, remote);
                if (container != null) {
                    matched.add(container.getId());
                }
                entries.add(CrmContainerEntry.builder()
                        .remoteId(remote.remoteId())
                        .name(remote.name())
                        .type(current)
                        .selected(container != null)
                        .containerId(container == null ? null : container.getId())
                        .existsInCrm(true)
                        .build());
            }
        }
        imported.stream()
                .filter(container -> !matched.contains(container.getId()))
                .forEach(container -> entries.add(CrmContainerEntry.builder()
                        .remoteId(container.getRemoteId())
                        .name(container.getName())
                        .type(container.getType())
                        .selected(true)
                        .containerId(container.getId())
                        .existsInCrm(false)
                        .build()));
        return entries;
    }

    public @NonNull List<CrmProperty> listProperties(@NonNull UUID customerId,
                                                     @NonNull UUID projectId,
                                                     @NonNull UUID connectionId,
                                                     @NonNull CrmContainerType type) throws NoTokenException {
        var session = this.sessionOpener.open(customerId, projectId, connectionId);
        return this.gatewayResolver.resolve(session).listProperties(session, type);
    }

    private static @Nullable CrmContainerEntity find(@NonNull List<CrmContainerEntity> imported,
                                                     @NonNull CrmContainerType type,
                                                     @NonNull CrmRemoteContainer remote) {
        return imported.stream()
                .filter(container -> container.getType() == type && container.getRemoteId().equals(remote.remoteId()))
                .findFirst()
                .orElse(null);
    }
}
