package com.bulldozer_collective.modules.crm.associations.services;

import com.bulldozer_collective.modules.crm.api.CrmObjectRef;
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
public class CrmAssociationDeleter {

    private final CrmSessionOpener sessionOpener;
    private final CrmGatewayResolver gatewayResolver;
    private final BulldozerLogger logger;

    /** Removes every association between the two records; the records themselves are left untouched. */
    public void dissociate(@NonNull UUID customerId,
                           @NonNull UUID projectId,
                           @NonNull UUID connectionId,
                           @NonNull CrmObjectRef from,
                           @NonNull CrmObjectRef to) throws NoTokenException {
        var session = this.sessionOpener.open(customerId, projectId, connectionId);
        this.gatewayResolver.resolve(session).dissociate(session, from, to);
        this.logger.info(op("CRM-004", customerId, projectId) + "Dissociated %s %s from %s %s on connection %s",
                from.type(), from.id(), to.type(), to.id(), connectionId);
    }
}
