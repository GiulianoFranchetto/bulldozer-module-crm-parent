package com.bulldozer_collective.modules.crm.landingpages.services;

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
public class CrmLandingPageDeleter {

    private final CrmSessionOpener sessionOpener;
    private final CrmGatewayResolver gatewayResolver;
    private final BulldozerLogger logger;

    /** Deletes the page in the CRM — a landing page is content, unlike a CRM record, which the bridge never deletes. */
    public void delete(@NonNull UUID customerId,
                       @NonNull UUID projectId,
                       @NonNull UUID connectionId,
                       @NonNull String id) throws NoTokenException {
        var session = this.sessionOpener.open(customerId, projectId, connectionId);
        this.gatewayResolver.resolve(session).deleteLandingPage(session, id);
        this.logger.info(op("CRM-008", customerId, projectId) + "Deleted landing page %s on connection %s",
                id, connectionId);
    }
}
