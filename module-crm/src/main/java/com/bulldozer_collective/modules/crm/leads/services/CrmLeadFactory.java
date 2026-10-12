package com.bulldozer_collective.modules.crm.leads.services;

import com.bulldozer_collective.modules.crm.api.CrmContainerType;
import com.bulldozer_collective.modules.crm.api.CrmLead;
import com.bulldozer_collective.modules.crm.containers.services.CrmContainerFetcher;
import com.bulldozer_collective.modules.crm.containers.services.CrmStageSources;
import com.bulldozer_collective.modules.crm.gateway.CrmGatewayResolver;
import com.bulldozer_collective.modules.crm.gateway.CrmSessionOpener;
import com.bulldozer_collective.modules.crm.gateway.NoTokenException;
import com.bulldozer_collective.logging.BulldozerLogger;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static com.bulldozer_collective.modules.crm.configurations.CrmLogContext.op;

@Service
@RequiredArgsConstructor
public class CrmLeadFactory {

    private final CrmContainerFetcher containerFetcher;
    private final CrmSessionOpener sessionOpener;
    private final CrmGatewayResolver gatewayResolver;
    private final CrmStageSources stageSources;
    private final CrmLeadSearcher searcher;
    private final BulldozerLogger logger;

    /**
     * Creates the lead in the container's pipeline, attached to its contact, with the stage value its mapping
     * designates, then reads it back.
     *
     * @throws IllegalArgumentException when the stage cannot be written in the container, or {@code pipelineStage}
     *                                  does not fit it (see {@code CrmStageSources#creationWrites}).
     */
    public @NonNull CrmLead create(@NonNull UUID customerId,
                                   @NonNull UUID projectId,
                                   @NonNull UUID connectionId,
                                   @NonNull UUID containerId,
                                   @NonNull CreateCrmLeadBuilder lead) throws NoTokenException {
        var container = this.containerFetcher.fetch(customerId, projectId, connectionId, containerId,
                CrmContainerType.CRM_CONTAINER_TYPE_LEAD);
        var stages = this.containerFetcher.leadStages(container);
        var stage = stages.write(lead.stage());

        var session = this.sessionOpener.open(customerId, projectId, connectionId);
        var gateway = this.gatewayResolver.resolve(session);
        var writes = this.stageSources.creationWrites(session, gateway, container, stage, lead.pipelineStage());
        var id = gateway.createLead(session, container.getRemoteId(), lead, writes);
        this.logger.info(op("CRM-004", customerId, projectId) + "Created lead %s in container %s", id, containerId);
        return this.searcher.read(session, gateway, container, stages, id);
    }
}
