package com.bulldozer_collective.modules.crm.leads.services;

import com.bulldozer_collective.modules.crm.api.CrmContainerType;
import com.bulldozer_collective.modules.crm.api.CrmLead;
import com.bulldozer_collective.modules.crm.api.CrmLeadStage;
import com.bulldozer_collective.modules.crm.api.CrmPage;
import com.bulldozer_collective.modules.crm.containers.data.CrmContainerEntity;
import com.bulldozer_collective.modules.crm.containers.services.CrmContainerFetcher;
import com.bulldozer_collective.modules.crm.containers.services.CrmStageTable;
import com.bulldozer_collective.modules.crm.gateway.CrmContainedRecord;
import com.bulldozer_collective.modules.crm.gateway.CrmGateway;
import com.bulldozer_collective.modules.crm.gateway.CrmGatewayResolver;
import com.bulldozer_collective.modules.crm.gateway.CrmSession;
import com.bulldozer_collective.modules.crm.gateway.CrmSessionOpener;
import com.bulldozer_collective.modules.crm.gateway.ListCrmObjectsBuilder;
import com.bulldozer_collective.modules.crm.gateway.NoTokenException;
import com.bulldozer_collective.shared.exceptions.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.util.UUID;

/** Reads the leads of a container from the CRM and translates their stage through the container's stages. */
@Service
@RequiredArgsConstructor
public class CrmLeadSearcher {

    private final CrmContainerFetcher containerFetcher;
    private final CrmSessionOpener sessionOpener;
    private final CrmGatewayResolver gatewayResolver;

    public @NonNull CrmPage<CrmLead> list(@NonNull UUID customerId,
                                          @NonNull UUID projectId,
                                          @NonNull UUID connectionId,
                                          @NonNull UUID containerId,
                                          @NonNull ListCrmObjectsBuilder query) throws NoTokenException {
        var container = this.containerFetcher.fetch(customerId, projectId, connectionId, containerId,
                CrmContainerType.CRM_CONTAINER_TYPE_LEAD);
        var stages = this.containerFetcher.leadStages(container);
        var session = this.sessionOpener.open(customerId, projectId, connectionId);
        var page = this.gatewayResolver.resolve(session)
                .listLeads(session, container.getRemoteId(), query, stages.propertyName());
        return new CrmPage<>(page.items().stream().map(record -> resolve(container, stages, record)).toList(),
                page.nextCursor());
    }

    /** @throws NotFoundException when the CRM has no such lead, or it belongs to another pipeline. */
    public @NonNull CrmLead get(@NonNull UUID customerId,
                                @NonNull UUID projectId,
                                @NonNull UUID connectionId,
                                @NonNull UUID containerId,
                                @NonNull String id) throws NoTokenException {
        var container = this.containerFetcher.fetch(customerId, projectId, connectionId, containerId,
                CrmContainerType.CRM_CONTAINER_TYPE_LEAD);
        var stages = this.containerFetcher.leadStages(container);
        var session = this.sessionOpener.open(customerId, projectId, connectionId);
        return this.read(session, this.gatewayResolver.resolve(session), container, stages, id);
    }

    /** For the factory, which already holds the container, its stages and a session. */
    @NonNull CrmLead read(@NonNull CrmSession session,
                          @NonNull CrmGateway gateway,
                          @NonNull CrmContainerEntity container,
                          @NonNull CrmStageTable<CrmLeadStage> stages,
                          @NonNull String id) {
        var record = gateway.getLead(session, id, stages.propertyName());
        if (!container.getRemoteId().equals(record.containerRemoteId())) {
            throw new NotFoundException("Lead %s is not in container %s".formatted(id, container.getId()));
        }
        return resolve(container, stages, record);
    }

    private static CrmLead resolve(@NonNull CrmContainerEntity container,
                                   @NonNull CrmStageTable<CrmLeadStage> stages,
                                   @NonNull CrmContainedRecord<CrmLead> record) {
        var value = stages.value(record.properties());
        return record.item().toBuilder()
                .containerId(container.getId())
                .stageValue(value)
                .stage(stages.stage(value))
                .build();
    }
}
