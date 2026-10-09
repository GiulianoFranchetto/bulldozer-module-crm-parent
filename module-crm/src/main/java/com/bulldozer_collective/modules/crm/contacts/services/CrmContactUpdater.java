package com.bulldozer_collective.modules.crm.contacts.services;

import com.bulldozer_collective.modules.crm.api.CrmContact;
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
public class CrmContactUpdater {

    private final CrmSessionOpener sessionOpener;
    private final CrmGatewayResolver gatewayResolver;
    private final BulldozerLogger logger;

    /** Writes the non-null fields to the CRM, then reads the contact back. */
    public @NonNull CrmContact update(@NonNull UUID customerId,
                                  @NonNull UUID projectId,
                                  @NonNull UUID connectionId,
                                  @NonNull String id,
                                  @NonNull SaveCrmContactBuilder contact) throws NoTokenException {
        if (contact.isEmpty()) {
            throw new IllegalArgumentException("Nothing to update: every field is null");
        }
        var session = this.sessionOpener.open(customerId, projectId, connectionId);
        var gateway = this.gatewayResolver.resolve(session);
        gateway.updateContact(session, id, contact);
        this.logger.info(op("CRM-004", customerId, projectId) + "Updated contact %s on connection %s", id, connectionId);
        return gateway.getContact(session, id);
    }
}
