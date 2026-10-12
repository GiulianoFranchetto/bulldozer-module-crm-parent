package com.bulldozer_collective.modules.crm.containers.services;

import com.bulldozer_collective.modules.crm.api.CrmContainerType;
import com.bulldozer_collective.modules.crm.api.CrmDealStage;
import com.bulldozer_collective.modules.crm.api.CrmLeadStage;
import com.bulldozer_collective.modules.crm.connections.services.CrmConnectionFetcher;
import com.bulldozer_collective.modules.crm.containers.data.CrmContainerEntity;
import com.bulldozer_collective.modules.crm.containers.data.CrmContainerRepository;
import com.bulldozer_collective.modules.crm.containers.data.CrmContainerStageEntity;
import com.bulldozer_collective.modules.crm.containers.data.CrmContainerStageRepository;
import com.bulldozer_collective.shared.exceptions.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CrmContainerFetcher {

    private final CrmConnectionFetcher connectionFetcher;
    private final CrmContainerRepository containerRepository;
    private final CrmContainerStageRepository stageRepository;

    /** @throws NotFoundException when the connection does not exist in the project. */
    public @NonNull List<CrmContainerEntity> fetchAll(@NonNull UUID customerId,
                                                      @NonNull UUID projectId,
                                                      @NonNull UUID connectionId,
                                                      @Nullable CrmContainerType type) {
        this.connectionFetcher.fetch(customerId, projectId, connectionId);
        return type == null
                ? this.containerRepository.findAllByCustomerIdAndProjectIdAndConnection_IdOrderByCreatedAtAsc(
                        customerId, projectId, connectionId)
                : this.containerRepository.findAllByCustomerIdAndProjectIdAndConnection_IdAndTypeOrderByCreatedAtAsc(
                        customerId, projectId, connectionId, type);
    }

    /** @throws NotFoundException when the container does not exist under that connection. */
    public @NonNull CrmContainerEntity fetch(@NonNull UUID customerId,
                                             @NonNull UUID projectId,
                                             @NonNull UUID connectionId,
                                             @NonNull UUID containerId) {
        return this.containerRepository
                .findByCustomerIdAndProjectIdAndConnection_IdAndId(customerId, projectId, connectionId, containerId)
                .orElseThrow(() -> new NotFoundException("No CRM container " + containerId + " on this connection"));
    }

    /**
     * @throws NotFoundException        when the container does not exist under that connection.
     * @throws IllegalArgumentException when it groups another kind of record.
     */
    public @NonNull CrmContainerEntity fetch(@NonNull UUID customerId,
                                             @NonNull UUID projectId,
                                             @NonNull UUID connectionId,
                                             @NonNull UUID containerId,
                                             @NonNull CrmContainerType expected) {
        var container = this.fetch(customerId, projectId, connectionId, containerId);
        if (container.getType() != expected) {
            throw new IllegalArgumentException("containerId: %s is a %s container, expected a %s one"
                    .formatted(containerId, container.getType(), expected));
        }
        return container;
    }

    public @NonNull List<CrmContainerStageEntity> fetchStages(@NonNull CrmContainerEntity container) {
        return this.stageRepository.findAllByCustomerIdAndProjectIdAndContainer_IdOrderByDisplayOrderAsc(
                container.getCustomerId(), container.getProjectId(), container.getId());
    }

    public @NonNull CrmStageTable<CrmDealStage> dealStages(@NonNull CrmContainerEntity container) {
        return CrmStageTable.deals(container, this.fetchStages(container));
    }

    public @NonNull CrmStageTable<CrmLeadStage> leadStages(@NonNull CrmContainerEntity container) {
        return CrmStageTable.leads(container, this.fetchStages(container));
    }
}
