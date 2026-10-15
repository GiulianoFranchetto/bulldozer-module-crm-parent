package com.bulldozer_collective.modules.crm.landingpages.services;

import com.bulldozer_collective.modules.crm.api.CrmLandingPage;
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
public class CrmLandingPageFactory {

    private final CrmSessionOpener sessionOpener;
    private final CrmGatewayResolver gatewayResolver;
    private final BulldozerLogger logger;

    /** Creates the page in the CRM as a draft, then reads it back so the answer is what the CRM holds. */
    public @NonNull CrmLandingPage create(@NonNull UUID customerId,
                                          @NonNull UUID projectId,
                                          @NonNull UUID connectionId,
                                          @NonNull SaveCrmLandingPageBuilder page) throws NoTokenException {
        if (page.name() == null || page.templatePath() == null || page.slug() == null) {
            throw new IllegalArgumentException("A landing page needs a name, a templatePath and a slug");
        }
        var session = this.sessionOpener.open(customerId, projectId, connectionId);
        var gateway = this.gatewayResolver.resolve(session);
        var id = gateway.createLandingPage(session, page);
        this.logger.info(op("CRM-008", customerId, projectId) + "Created landing page %s on connection %s",
                id, connectionId);
        return gateway.getLandingPage(session, id);
    }
}
