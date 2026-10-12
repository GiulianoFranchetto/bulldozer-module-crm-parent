package com.bulldozer_collective.modules.crm.containers.services;

import com.bulldozer_collective.modules.crm.api.CrmContainerType;
import com.bulldozer_collective.modules.crm.containers.data.CrmContainerStageEntity;
import com.bulldozer_collective.modules.crm.containers.data.CrmContainerStageRepository;
import com.bulldozer_collective.shared.exceptions.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CrmContainerStageUpdater {

    private final CrmContainerFetcher fetcher;
    private final CrmContainerStageRepository stageRepository;
    private final CrmContainerPersister persister;

    /**
     * Maps one stage of the container to a Bulldozer stage, or unmaps it.
     *
     * @throws IllegalArgumentException when the stage given does not match the container's type.
     */
    public @NonNull CrmContainerStageEntity map(@NonNull UUID customerId,
                                                @NonNull UUID projectId,
                                                @NonNull UUID connectionId,
                                                @NonNull UUID containerId,
                                                @NonNull UUID stageId,
                                                @NonNull MapCrmContainerStageBuilder mapping) {
        var container = this.fetcher.fetch(customerId, projectId, connectionId, containerId);
        var deal = container.getType() == CrmContainerType.CRM_CONTAINER_TYPE_DEAL;
        if (deal ? mapping.leadStage() != null : mapping.dealStage() != null) {
            throw new IllegalArgumentException(deal
                    ? "leadStage: not allowed in a deal container, give dealStage"
                    : "dealStage: not allowed in a lead container, give leadStage");
        }

        var stage = this.stageRepository
                .findByCustomerIdAndProjectIdAndContainer_IdAndId(customerId, projectId, containerId, stageId)
                .orElseThrow(() -> new NotFoundException("No stage " + stageId + " in container " + containerId));
        return this.persister.map(containerId, stage, mapping);
    }
}
