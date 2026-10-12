package com.bulldozer_collective.modules.crm.containers.services;

import com.bulldozer_collective.modules.crm.api.CrmContainerType;
import com.bulldozer_collective.modules.crm.containers.data.CrmContainerStageEntity;
import com.bulldozer_collective.shared.exceptions.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CrmContainerStageUpdater {

    private final CrmContainerFetcher fetcher;
    private final CrmContainerPersister persister;

    /**
     * Replaces the mapping of every listed stage of the container, all or none. Unlisted stages are left as they
     * are, except that a write default given here takes the mark off the other stages mapped to the same stage.
     *
     * @return every stage of the container, after the update.
     * @throws NotFoundException        when a stage is not in the container.
     * @throws IllegalArgumentException when a mapping does not match the container's type, a stage is listed twice,
     *                                  or two write defaults are given for the same stage.
     */
    public @NonNull List<CrmContainerStageEntity> map(@NonNull UUID customerId,
                                                      @NonNull UUID projectId,
                                                      @NonNull UUID connectionId,
                                                      @NonNull UUID containerId,
                                                      @NonNull List<MapCrmContainerStageBuilder> mappings) {
        var container = this.fetcher.fetch(customerId, projectId, connectionId, containerId);
        var deal = container.getType() == CrmContainerType.CRM_CONTAINER_TYPE_DEAL;

        var stageIds = new HashSet<UUID>();
        var defaults = new HashSet<Object>();
        for (var mapping : mappings) {
            if (deal ? mapping.leadStage() != null : mapping.dealStage() != null) {
                throw new IllegalArgumentException(deal
                        ? "leadStage: not allowed in a deal container, give dealStage"
                        : "dealStage: not allowed in a lead container, give leadStage");
            }
            if (!stageIds.add(mapping.stageId())) {
                throw new IllegalArgumentException("mappings: stage %s is listed twice".formatted(mapping.stageId()));
            }
            var target = deal ? mapping.dealStage() : mapping.leadStage();
            if (mapping.writeDefault() && target != null && !defaults.add(target)) {
                throw new IllegalArgumentException(
                        "mappings: several write defaults for %s; mark only one".formatted(target));
            }
        }

        return this.persister.map(customerId, projectId, containerId, mappings);
    }
}
