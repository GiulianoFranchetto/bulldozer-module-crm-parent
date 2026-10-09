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
public class CrmAssociationFactory {

    private final CrmSessionOpener sessionOpener;
    private final CrmGatewayResolver gatewayResolver;
    private final BulldozerLogger logger;

    /** Associates two CRM records. Idempotent on the CRM's side: associating twice changes nothing. */
    public void associate(@NonNull UUID customerId,
                          @NonNull UUID projectId,
                          @NonNull UUID connectionId,
                          @NonNull CrmObjectRef from,
                          @NonNull CrmObjectRef to) throws NoTokenException {
        if (from.equals(to)) {
            throw new IllegalArgumentException("A record cannot be associated with itself");
        }
        var session = this.sessionOpener.open(customerId, projectId, connectionId);
        this.gatewayResolver.resolve(session).associate(session, from, to);
        this.logger.info(op("CRM-004", customerId, projectId) + "Associated %s %s with %s %s on connection %s",
                from.type(), from.id(), to.type(), to.id(), connectionId);
    }
}
