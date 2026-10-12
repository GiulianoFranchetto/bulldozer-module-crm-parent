package com.bulldozer_collective.modules.crm.deals.services;

import com.bulldozer_collective.modules.crm.api.CrmContainerType;
import com.bulldozer_collective.modules.crm.api.CrmDeal;
import com.bulldozer_collective.modules.crm.api.CrmDealStage;
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

/** Reads the deals of a container from the CRM and translates their stage through the container's stages. */
@Service
@RequiredArgsConstructor
public class CrmDealSearcher {

    private final CrmContainerFetcher containerFetcher;
    private final CrmSessionOpener sessionOpener;
    private final CrmGatewayResolver gatewayResolver;

    public @NonNull CrmPage<CrmDeal> list(@NonNull UUID customerId,
                                          @NonNull UUID projectId,
                                          @NonNull UUID connectionId,
                                          @NonNull UUID containerId,
                                          @NonNull ListCrmObjectsBuilder query) throws NoTokenException {
        var container = this.containerFetcher.fetch(customerId, projectId, connectionId, containerId,
                CrmContainerType.CRM_CONTAINER_TYPE_DEAL);
        var stages = this.containerFetcher.dealStages(container);
        var session = this.sessionOpener.open(customerId, projectId, connectionId);
        var page = this.gatewayResolver.resolve(session)
                .listDeals(session, container.getRemoteId(), query, stages.propertyName());
        return new CrmPage<>(page.items().stream().map(record -> resolve(container, stages, record)).toList(),
                page.nextCursor());
    }

    /** @throws NotFoundException when the CRM has no such deal, or it belongs to another pipeline. */
    public @NonNull CrmDeal get(@NonNull UUID customerId,
                                @NonNull UUID projectId,
                                @NonNull UUID connectionId,
                                @NonNull UUID containerId,
                                @NonNull String id) throws NoTokenException {
        var container = this.containerFetcher.fetch(customerId, projectId, connectionId, containerId,
                CrmContainerType.CRM_CONTAINER_TYPE_DEAL);
        var stages = this.containerFetcher.dealStages(container);
        var session = this.sessionOpener.open(customerId, projectId, connectionId);
        return this.read(session, this.gatewayResolver.resolve(session), container, stages, id);
    }

    /** For the writers, which already hold the container, its stages and a session. */
    @NonNull CrmDeal read(@NonNull CrmSession session,
                          @NonNull CrmGateway gateway,
                          @NonNull CrmContainerEntity container,
                          @NonNull CrmStageTable<CrmDealStage> stages,
                          @NonNull String id) {
        var record = gateway.getDeal(session, id, stages.propertyName());
        if (!container.getRemoteId().equals(record.containerRemoteId())) {
            throw new NotFoundException("Deal %s is not in container %s".formatted(id, container.getId()));
        }
        return resolve(container, stages, record);
    }

    private static CrmDeal resolve(@NonNull CrmContainerEntity container,
                                   @NonNull CrmStageTable<CrmDealStage> stages,
                                   @NonNull CrmContainedRecord<CrmDeal> record) {
        var value = stages.value(record.properties());
        return record.item().toBuilder()
                .containerId(container.getId())
                .stageValue(value)
                .stage(stages.stage(value))
                .build();
    }
}
