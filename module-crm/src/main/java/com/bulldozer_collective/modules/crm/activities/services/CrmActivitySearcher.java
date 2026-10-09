package com.bulldozer_collective.modules.crm.activities.services;

import com.bulldozer_collective.modules.crm.api.CrmActivity;
import com.bulldozer_collective.modules.crm.api.CrmActivityType;
import com.bulldozer_collective.modules.crm.api.CrmPage;
import com.bulldozer_collective.modules.crm.gateway.CrmGatewayResolver;
import com.bulldozer_collective.modules.crm.gateway.CrmSessionOpener;
import com.bulldozer_collective.modules.crm.gateway.ListCrmObjectsBuilder;
import com.bulldozer_collective.modules.crm.gateway.NoTokenException;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CrmActivitySearcher {

    private final CrmSessionOpener sessionOpener;
    private final CrmGatewayResolver gatewayResolver;

    public @NonNull CrmPage<CrmActivity> list(@NonNull UUID customerId,
                                              @NonNull UUID projectId,
                                              @NonNull UUID connectionId,
                                              @NonNull CrmActivityType type,
                                              @NonNull ListCrmObjectsBuilder query) throws NoTokenException {
        var session = this.sessionOpener.open(customerId, projectId, connectionId);
        return this.gatewayResolver.resolve(session).listActivities(session, type, query);
    }

    public @NonNull CrmActivity get(@NonNull UUID customerId,
                                    @NonNull UUID projectId,
                                    @NonNull UUID connectionId,
                                    @NonNull CrmActivityType type,
                                    @NonNull String id) throws NoTokenException {
        var session = this.sessionOpener.open(customerId, projectId, connectionId);
        return this.gatewayResolver.resolve(session).getActivity(session, type, id);
    }
}
