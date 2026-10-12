package com.bulldozer_collective.modules.crm.deals.services;

import com.bulldozer_collective.modules.crm.api.CrmContainerType;
import com.bulldozer_collective.modules.crm.api.CrmDeal;
import com.bulldozer_collective.modules.crm.containers.services.CrmContainerFetcher;
import com.bulldozer_collective.modules.crm.gateway.CrmGatewayResolver;
import com.bulldozer_collective.modules.crm.gateway.CrmSessionOpener;
import com.bulldozer_collective.modules.crm.gateway.NoTokenException;
import com.bulldozer_collective.logging.BulldozerLogger;
import com.bulldozer_collective.shared.exceptions.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static com.bulldozer_collective.modules.crm.configurations.CrmLogContext.op;

@Service
@RequiredArgsConstructor
public class CrmDealUpdater {

    private final CrmContainerFetcher containerFetcher;
    private final CrmSessionOpener sessionOpener;
    private final CrmGatewayResolver gatewayResolver;
    private final CrmDealSearcher searcher;
    private final BulldozerLogger logger;

    /**
     * Writes the non-null fields to the CRM, then reads the deal back. The deal stays in its container: a stage is
     * written as the value its container's mapping designates.
     *
     * @throws NotFoundException        when the deal is not in the container.
     * @throws IllegalArgumentException when nothing is to be written, or the stage cannot be written.
     */
    public @NonNull CrmDeal update(@NonNull UUID customerId,
                                   @NonNull UUID projectId,
                                   @NonNull UUID connectionId,
                                   @NonNull UUID containerId,
                                   @NonNull String id,
                                   @NonNull SaveCrmDealBuilder deal) throws NoTokenException {
        if (deal.isEmpty()) {
            throw new IllegalArgumentException("Nothing to update: every field is null");
        }
        var container = this.containerFetcher.fetch(customerId, projectId, connectionId, containerId,
                CrmContainerType.CRM_CONTAINER_TYPE_DEAL);
        var stages = this.containerFetcher.dealStages(container);
        var stage = deal.stage() == null ? null : stages.write(deal.stage());

        var session = this.sessionOpener.open(customerId, projectId, connectionId);
        var gateway = this.gatewayResolver.resolve(session);
        // Refuse to write a deal of another pipeline through this container.
        this.searcher.read(session, gateway, container, stages, id);

        gateway.updateDeal(session, id, deal, stage);
        this.logger.info(op("CRM-004", customerId, projectId) + "Updated deal %s in container %s", id, containerId);
        return this.searcher.read(session, gateway, container, stages, id);
    }
}
