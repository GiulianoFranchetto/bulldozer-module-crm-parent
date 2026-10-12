package com.bulldozer_collective.modules.crm.containers.services;

import com.bulldozer_collective.modules.crm.api.CrmContainerType;
import com.bulldozer_collective.modules.crm.api.CrmProperty;
import com.bulldozer_collective.modules.crm.api.CrmRemoteContainer;
import com.bulldozer_collective.modules.crm.gateway.CrmGatewayResolver;
import com.bulldozer_collective.modules.crm.gateway.CrmSessionOpener;
import com.bulldozer_collective.modules.crm.gateway.NoTokenException;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/** What an import is chosen from: the CRM's pipelines and properties, read live. */
@Service
@RequiredArgsConstructor
public class CrmRemoteContainerSearcher {

    private final CrmSessionOpener sessionOpener;
    private final CrmGatewayResolver gatewayResolver;

    public @NonNull List<CrmRemoteContainer> listContainers(@NonNull UUID customerId,
                                                            @NonNull UUID projectId,
                                                            @NonNull UUID connectionId,
                                                            @NonNull CrmContainerType type) throws NoTokenException {
        var session = this.sessionOpener.open(customerId, projectId, connectionId);
        return this.gatewayResolver.resolve(session).listRemoteContainers(session, type);
    }

    public @NonNull List<CrmProperty> listProperties(@NonNull UUID customerId,
                                                     @NonNull UUID projectId,
                                                     @NonNull UUID connectionId,
                                                     @NonNull CrmContainerType type) throws NoTokenException {
        var session = this.sessionOpener.open(customerId, projectId, connectionId);
        return this.gatewayResolver.resolve(session).listProperties(session, type);
    }
}
