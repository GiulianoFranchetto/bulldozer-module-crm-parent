package com.bulldozer_collective.modules.crm.forms.services;

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
public class CrmFormDeleter {

    private final CrmSessionOpener sessionOpener;
    private final CrmGatewayResolver gatewayResolver;
    private final BulldozerLogger logger;

    /** Deletes the form in the CRM; the contacts its submissions created stay. */
    public void delete(@NonNull UUID customerId,
                       @NonNull UUID projectId,
                       @NonNull UUID connectionId,
                       @NonNull String id) throws NoTokenException {
        var session = this.sessionOpener.open(customerId, projectId, connectionId);
        this.gatewayResolver.resolve(session).deleteForm(session, id);
        this.logger.info(op("CRM-009", customerId, projectId) + "Deleted form %s on connection %s", id, connectionId);
    }
}
