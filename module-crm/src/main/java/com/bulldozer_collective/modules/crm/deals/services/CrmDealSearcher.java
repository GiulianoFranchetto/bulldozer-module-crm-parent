package com.bulldozer_collective.modules.crm.deals.services;

import com.bulldozer_collective.modules.crm.api.CrmDeal;
import com.bulldozer_collective.modules.crm.api.CrmPage;
import com.bulldozer_collective.modules.crm.gateway.CrmGateway;
import com.bulldozer_collective.modules.crm.gateway.CrmGatewayResolver;
import com.bulldozer_collective.modules.crm.gateway.CrmSession;
import com.bulldozer_collective.modules.crm.gateway.CrmSessionOpener;
import com.bulldozer_collective.modules.crm.gateway.ListCrmObjectsBuilder;
import com.bulldozer_collective.modules.crm.gateway.NoTokenException;
import com.bulldozer_collective.modules.crm.stages.services.CrmStageResolver;
import com.bulldozer_collective.modules.crm.stages.services.CrmStageTable;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.util.UUID;

/** Reads deals from the CRM and translates their stage through the project's stage mappings. */
@Service
@RequiredArgsConstructor
public class CrmDealSearcher {

    private final CrmSessionOpener sessionOpener;
    private final CrmGatewayResolver gatewayResolver;
    private final CrmStageResolver stageResolver;

    public @NonNull CrmPage<CrmDeal> list(@NonNull UUID customerId,
                                          @NonNull UUID projectId,
                                          @NonNull UUID connectionId,
                                          @NonNull ListCrmObjectsBuilder query) throws NoTokenException {
        var session = this.sessionOpener.open(customerId, projectId, connectionId);
        var stages = this.stageResolver.load(customerId, projectId, connectionId);
        var page = this.gatewayResolver.resolve(session).listDeals(session, query, stages.propertyNames());
        return new CrmPage<>(page.items().stream().map(stages::resolve).toList(), page.nextCursor());
    }

    public @NonNull CrmDeal get(@NonNull UUID customerId,
                                @NonNull UUID projectId,
                                @NonNull UUID connectionId,
                                @NonNull String id) throws NoTokenException {
        var session = this.sessionOpener.open(customerId, projectId, connectionId);
        var stages = this.stageResolver.load(customerId, projectId, connectionId);
        return this.read(session, this.gatewayResolver.resolve(session), stages, id);
    }

    /** For the writers, which already hold a session and the stage table. */
    @NonNull CrmDeal read(@NonNull CrmSession session,
                          @NonNull CrmGateway gateway,
                          @NonNull CrmStageTable stages,
                          @NonNull String id) {
        return stages.resolve(gateway.getDeal(session, id, stages.propertyNames()));
    }
}
