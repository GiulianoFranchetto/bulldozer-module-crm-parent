package com.bulldozer_collective.modules.crm.containers.services;

import com.bulldozer_collective.modules.crm.api.CrmContainerEntry;
import com.bulldozer_collective.modules.crm.api.CrmContainerEntryStage;
import com.bulldozer_collective.modules.crm.api.CrmContainerType;
import com.bulldozer_collective.modules.crm.api.CrmProperty;
import com.bulldozer_collective.modules.crm.containers.data.CrmContainerEntity;
import com.bulldozer_collective.modules.crm.gateway.CrmGatewayResolver;
import com.bulldozer_collective.modules.crm.gateway.CrmRemoteContainer;
import com.bulldozer_collective.modules.crm.gateway.CrmSessionOpener;
import com.bulldozer_collective.modules.crm.gateway.NoTokenException;
import com.bulldozer_collective.logging.BulldozerLogger;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import static com.bulldozer_collective.modules.crm.configurations.CrmLogContext.op;

/** What the project chooses its containers from: the CRM's containers and properties, read live. */
@Service
@RequiredArgsConstructor
public class CrmContainerSearcher {

    private final CrmContainerFetcher fetcher;
    private final CrmSessionOpener sessionOpener;
    private final CrmGatewayResolver gatewayResolver;
    private final BulldozerLogger logger;

    /**
     * Every container of the CRM, flagged {@code selected} with its {@code containerId} when the project imported
     * it, in the CRM's order; then the imported containers the CRM no longer has.
     *
     * @param type {@code null} lists the containers of deals and of leads. A type the CRM refuses to list — no
     *             leads scope, no lead pipelines on the account's subscription — is then left out rather than
     *             failing the whole listing, its imported containers included; it fails only when every type does.
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
        var refused = EnumSet.noneOf(CrmContainerType.class);
        RuntimeException firstFailure = null;
        var listed = 0;
        for (var current : types) {
            List<CrmRemoteContainer> remotes;
            try {
                remotes = gateway.listRemoteContainers(session, current);
                listed++;
            } catch (RuntimeException e) {
                if (type != null) {
                    throw e;
                }
                this.logger.warn(op("CRM-003", customerId, projectId)
                        + "Leaving %s containers out of the listing: the CRM refused them", current, e);
                refused.add(current);
                firstFailure = firstFailure == null ? e : firstFailure;
                continue;
            }
            for (var remote : remotes) {
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
                        .stages(remote.stages().stream()
                                .map(stage -> CrmContainerEntryStage.builder()
                                        .remoteId(stage.remoteId())
                                        .name(stage.name())
                                        .displayOrder(stage.displayOrder())
                                        .build())
                                .toList())
                        .build());
            }
        }
        if (listed == 0 && firstFailure != null) {
            throw firstFailure;
        }
        // A refused type says nothing about its containers: only a type the CRM listed can prove one gone.
        imported.stream()
                .filter(container -> !matched.contains(container.getId()) && !refused.contains(container.getType()))
                .forEach(container -> entries.add(CrmContainerEntry.builder()
                        .remoteId(container.getRemoteId())
                        .name(container.getName())
                        .type(container.getType())
                        .selected(true)
                        .containerId(container.getId())
                        .existsInCrm(false)
                        .stages(List.of())
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
