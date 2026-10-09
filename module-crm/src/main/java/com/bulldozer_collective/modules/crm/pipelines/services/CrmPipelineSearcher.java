package com.bulldozer_collective.modules.crm.pipelines.services;

import com.bulldozer_collective.modules.crm.api.CrmDealProperty;
import com.bulldozer_collective.modules.crm.api.CrmPipeline;
import com.bulldozer_collective.modules.crm.gateway.CrmGatewayResolver;
import com.bulldozer_collective.modules.crm.gateway.CrmSessionOpener;
import com.bulldozer_collective.modules.crm.gateway.NoTokenException;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/** What the stage configuration is built on: the CRM's deal pipelines and deal properties. */
@Service
@RequiredArgsConstructor
public class CrmPipelineSearcher {

    private final CrmSessionOpener sessionOpener;
    private final CrmGatewayResolver gatewayResolver;

    public @NonNull List<CrmPipeline> listPipelines(@NonNull UUID customerId,
                                                    @NonNull UUID projectId,
                                                    @NonNull UUID connectionId) throws NoTokenException {
        var session = this.sessionOpener.open(customerId, projectId, connectionId);
        return this.gatewayResolver.resolve(session).listPipelines(session);
    }

    public @NonNull List<CrmDealProperty> listDealProperties(@NonNull UUID customerId,
                                                             @NonNull UUID projectId,
                                                             @NonNull UUID connectionId) throws NoTokenException {
        var session = this.sessionOpener.open(customerId, projectId, connectionId);
        return this.gatewayResolver.resolve(session).listDealProperties(session);
    }
}
