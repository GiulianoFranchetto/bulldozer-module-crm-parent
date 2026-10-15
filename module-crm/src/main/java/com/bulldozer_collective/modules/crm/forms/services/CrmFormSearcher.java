package com.bulldozer_collective.modules.crm.forms.services;

import com.bulldozer_collective.modules.crm.api.CrmForm;
import com.bulldozer_collective.modules.crm.api.CrmPage;
import com.bulldozer_collective.modules.crm.gateway.CrmGatewayResolver;
import com.bulldozer_collective.modules.crm.gateway.CrmSessionOpener;
import com.bulldozer_collective.modules.crm.gateway.NoTokenException;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CrmFormSearcher {

    private final CrmSessionOpener sessionOpener;
    private final CrmGatewayResolver gatewayResolver;

    public @NonNull CrmPage<CrmForm> list(@NonNull UUID customerId,
                                          @NonNull UUID projectId,
                                          @NonNull UUID connectionId,
                                          @Nullable String cursor,
                                          int limit) throws NoTokenException {
        var session = this.sessionOpener.open(customerId, projectId, connectionId);
        return this.gatewayResolver.resolve(session).listForms(session, cursor, limit);
    }

    public @NonNull CrmForm get(@NonNull UUID customerId,
                                @NonNull UUID projectId,
                                @NonNull UUID connectionId,
                                @NonNull String id) throws NoTokenException {
        var session = this.sessionOpener.open(customerId, projectId, connectionId);
        return this.gatewayResolver.resolve(session).getForm(session, id);
    }
}
