package com.bulldozer_collective.modules.crm.companies.services;

import com.bulldozer_collective.modules.crm.api.CrmCompany;
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
public class CrmCompanyFactory {

    private final CrmSessionOpener sessionOpener;
    private final CrmGatewayResolver gatewayResolver;
    private final BulldozerLogger logger;

    /** Creates the company in the CRM, then reads it back so the answer is what the CRM holds. */
    public @NonNull CrmCompany create(@NonNull UUID customerId,
                                  @NonNull UUID projectId,
                                  @NonNull UUID connectionId,
                                  @NonNull SaveCrmCompanyBuilder company) throws NoTokenException {
        if (company.isEmpty()) {
            throw new IllegalArgumentException("A company needs at least one field");
        }
        var session = this.sessionOpener.open(customerId, projectId, connectionId);
        var gateway = this.gatewayResolver.resolve(session);
        var id = gateway.createCompany(session, company);
        this.logger.info(op("CRM-004", customerId, projectId) + "Created company %s on connection %s", id, connectionId);
        return gateway.getCompany(session, id);
    }
}
