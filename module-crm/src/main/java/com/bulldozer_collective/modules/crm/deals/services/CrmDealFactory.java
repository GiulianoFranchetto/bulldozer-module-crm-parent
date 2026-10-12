package com.bulldozer_collective.modules.crm.deals.services;

import com.bulldozer_collective.modules.crm.api.CrmContainerType;
import com.bulldozer_collective.modules.crm.api.CrmDeal;
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
public class CrmDealFactory {

    private final CrmContainerFetcher containerFetcher;
    private final CrmSessionOpener sessionOpener;
    private final CrmGatewayResolver gatewayResolver;
    private final CrmStageSources stageSources;
    private final CrmDealSearcher searcher;
    private final BulldozerLogger logger;

    /**
     * Creates the deal in the container's pipeline, with the stage value its mapping designates, then reads it back.
     *
     * @throws IllegalArgumentException when name or stage is missing, the stage cannot be written in the
     *                                  container (see {@code CrmStageTable#write}), or {@code pipelineStage} does not
     *                                  fit the container (see {@code CrmStageSources#creationWrites}).
     */
    public @NonNull CrmDeal create(@NonNull UUID customerId,
                                   @NonNull UUID projectId,
                                   @NonNull UUID connectionId,
                                   @NonNull UUID containerId,
                                   @NonNull SaveCrmDealBuilder deal) throws NoTokenException {
        if (deal.name() == null || deal.stage() == null) {
            throw new IllegalArgumentException("A deal needs a name and a stage");
        }
        var container = this.containerFetcher.fetch(customerId, projectId, connectionId, containerId,
                CrmContainerType.CRM_CONTAINER_TYPE_DEAL);
        var stages = this.containerFetcher.dealStages(container);
        var stage = stages.write(deal.stage());

        var session = this.sessionOpener.open(customerId, projectId, connectionId);
        var gateway = this.gatewayResolver.resolve(session);
        var writes = this.stageSources.creationWrites(session, gateway, container, stage, deal.pipelineStage());
        var id = gateway.createDeal(session, container.getRemoteId(), deal, writes);
        this.logger.info(op("CRM-004", customerId, projectId) + "Created deal %s in container %s", id, containerId);
        return this.searcher.read(session, gateway, container, stages, id);
    }
}
