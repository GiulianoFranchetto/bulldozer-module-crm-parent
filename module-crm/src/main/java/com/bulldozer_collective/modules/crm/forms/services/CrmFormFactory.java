package com.bulldozer_collective.modules.crm.forms.services;

import com.bulldozer_collective.modules.crm.api.CrmForm;
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
public class CrmFormFactory {

    private final CrmSessionOpener sessionOpener;
    private final CrmGatewayResolver gatewayResolver;
    private final BulldozerLogger logger;

    /** Creates the form in the CRM, then reads it back so the answer is what the CRM holds. */
    public @NonNull CrmForm create(@NonNull UUID customerId,
                                   @NonNull UUID projectId,
                                   @NonNull UUID connectionId,
                                   @NonNull SaveCrmFormBuilder form) throws NoTokenException {
        if (form.name() == null || form.fields() == null || form.fields().isEmpty()) {
            throw new IllegalArgumentException("A form needs a name and at least one field");
        }
        CrmFormValidator.validate(form);
        var session = this.sessionOpener.open(customerId, projectId, connectionId);
        var gateway = this.gatewayResolver.resolve(session);
        var id = gateway.createForm(session, form);
        this.logger.info(op("CRM-009", customerId, projectId) + "Created form %s on connection %s", id, connectionId);
        return gateway.getForm(session, id);
    }
}
