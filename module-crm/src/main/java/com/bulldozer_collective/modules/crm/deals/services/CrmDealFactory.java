package com.bulldozer_collective.modules.crm.deals.services;

import com.bulldozer_collective.modules.crm.api.CrmDeal;
import com.bulldozer_collective.modules.crm.gateway.CrmGatewayResolver;
import com.bulldozer_collective.modules.crm.gateway.CrmSessionOpener;
import com.bulldozer_collective.modules.crm.gateway.NoTokenException;
import com.bulldozer_collective.modules.crm.stages.services.CrmStageResolver;
import com.bulldozer_collective.logging.BulldozerLogger;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static com.bulldozer_collective.modules.crm.configurations.CrmLogContext.op;

@Service
@RequiredArgsConstructor
public class CrmDealFactory {

    private final CrmSessionOpener sessionOpener;
    private final CrmGatewayResolver gatewayResolver;
    private final CrmStageResolver stageResolver;
    private final CrmDealSearcher searcher;
    private final BulldozerLogger logger;

    /**
     * Creates the deal in the CRM with the stage value its mapping designates, then reads it back.
     *
     * @throws IllegalArgumentException when name, pipeline or stage is missing, or the stage cannot be written in
     *                                  that pipeline (see {@code CrmStageTable#write}).
     */
    public @NonNull CrmDeal create(@NonNull UUID customerId,
                                   @NonNull UUID projectId,
                                   @NonNull UUID connectionId,
                                   @NonNull SaveCrmDealBuilder deal) throws NoTokenException {
        if (deal.name() == null || deal.pipelineId() == null || deal.stage() == null) {
            throw new IllegalArgumentException("A deal needs a name, a pipelineId and a stage");
        }
        var session = this.sessionOpener.open(customerId, projectId, connectionId);
        var stages = this.stageResolver.load(customerId, projectId, connectionId);
        var stage = stages.write(deal.pipelineId(), deal.stage());

        var gateway = this.gatewayResolver.resolve(session);
        var id = gateway.createDeal(session, deal, stage);
        this.logger.info(op("CRM-004", customerId, projectId) + "Created deal %s on connection %s", id, connectionId);
        return this.searcher.read(session, gateway, stages, id);
    }
}
