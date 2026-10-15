package com.bulldozer_collective.modules.crm.landingpages.services;

import com.bulldozer_collective.modules.crm.api.CrmLandingPage;
import com.bulldozer_collective.modules.crm.gateway.CrmGatewayResolver;
import com.bulldozer_collective.modules.crm.gateway.CrmSessionOpener;
import com.bulldozer_collective.modules.crm.gateway.NoTokenException;
import com.bulldozer_collective.logging.BulldozerLogger;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

import static com.bulldozer_collective.modules.crm.configurations.CrmLogContext.op;

@Service
@RequiredArgsConstructor
public class CrmLandingPageUpdater {

    private final CrmSessionOpener sessionOpener;
    private final CrmGatewayResolver gatewayResolver;
    private final BulldozerLogger logger;

    /** Writes the non-null fields to the CRM, then reads the page back. */
    public @NonNull CrmLandingPage update(@NonNull UUID customerId,
                                          @NonNull UUID projectId,
                                          @NonNull UUID connectionId,
                                          @NonNull String id,
                                          @NonNull SaveCrmLandingPageBuilder page) throws NoTokenException {
        if (page.isEmpty()) {
            throw new IllegalArgumentException("Nothing to update: every field is null");
        }
        if (page.templatePath() != null) {
            throw new IllegalArgumentException("templatePath: a landing page keeps the template it was created on");
        }
        var session = this.sessionOpener.open(customerId, projectId, connectionId);
        var gateway = this.gatewayResolver.resolve(session);
        gateway.updateLandingPage(session, id, page);
        this.logger.info(op("CRM-008", customerId, projectId) + "Updated landing page %s on connection %s",
                id, connectionId);
        return gateway.getLandingPage(session, id);
    }

    /** Publishes the page at {@code publishAt} — now when {@code null} — then reads it back. */
    public @NonNull CrmLandingPage publish(@NonNull UUID customerId,
                                           @NonNull UUID projectId,
                                           @NonNull UUID connectionId,
                                           @NonNull String id,
                                           @Nullable Instant publishAt) throws NoTokenException {
        var session = this.sessionOpener.open(customerId, projectId, connectionId);
        var gateway = this.gatewayResolver.resolve(session);
        var at = publishAt == null ? Instant.now() : publishAt;
        gateway.publishLandingPage(session, id, at);
        this.logger.info(op("CRM-008", customerId, projectId) + "Scheduled landing page %s for %s on connection %s",
                id, at, connectionId);
        return gateway.getLandingPage(session, id);
    }
}
