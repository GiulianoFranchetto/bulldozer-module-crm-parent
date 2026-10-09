package com.bulldozer_collective.modules.crm.deals.services;

import com.bulldozer_collective.modules.crm.api.CrmDeal;
import com.bulldozer_collective.modules.crm.gateway.CrmGatewayResolver;
import com.bulldozer_collective.modules.crm.gateway.CrmSessionOpener;
import com.bulldozer_collective.modules.crm.gateway.CrmStageWrite;
import com.bulldozer_collective.modules.crm.gateway.NoTokenException;
import com.bulldozer_collective.modules.crm.stages.services.CrmStageResolver;
import com.bulldozer_collective.logging.BulldozerLogger;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

import static com.bulldozer_collective.modules.crm.configurations.CrmLogContext.op;

@Service
@RequiredArgsConstructor
public class CrmDealUpdater {

    private final CrmSessionOpener sessionOpener;
    private final CrmGatewayResolver gatewayResolver;
    private final CrmStageResolver stageResolver;
    private final CrmDealSearcher searcher;
    private final BulldozerLogger logger;

    /**
     * Writes the non-null fields to the CRM, then reads the deal back. A stage is written in the pipeline the
     * request names, or else in the deal's current one.
     *
     * @throws IllegalArgumentException when nothing is to be written, when {@code pipelineId} comes without a
     *                                  {@code stage}, or when the stage cannot be written in that pipeline.
     */
    public @NonNull CrmDeal update(@NonNull UUID customerId,
                                   @NonNull UUID projectId,
                                   @NonNull UUID connectionId,
                                   @NonNull String id,
                                   @NonNull SaveCrmDealBuilder deal) throws NoTokenException {
        if (deal.isEmpty()) {
            throw new IllegalArgumentException("Nothing to update: every field is null");
        }
        if (deal.pipelineId() != null && deal.stage() == null) {
            throw new IllegalArgumentException("Moving a deal to another pipeline requires its stage in that pipeline");
        }

        var session = this.sessionOpener.open(customerId, projectId, connectionId);
        var gateway = this.gatewayResolver.resolve(session);
        var stages = this.stageResolver.load(customerId, projectId, connectionId);

        CrmStageWrite stage = null;
        if (deal.stage() != null) {
            var pipelineId = deal.pipelineId() != null
                    ? deal.pipelineId()
                    : gateway.getDeal(session, id, List.of()).deal().pipelineId();
            if (pipelineId == null) {
                throw new IllegalArgumentException("Deal %s has no pipeline: give a pipelineId with the stage"
                        .formatted(id));
            }
            stage = stages.write(pipelineId, deal.stage());
        }

        gateway.updateDeal(session, id, deal, stage);
        this.logger.info(op("CRM-004", customerId, projectId) + "Updated deal %s on connection %s", id, connectionId);
        return this.searcher.read(session, gateway, stages, id);
    }
}
