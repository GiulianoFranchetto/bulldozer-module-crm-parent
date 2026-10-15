package com.bulldozer_collective.modules.crm.forms.services;

import com.bulldozer_collective.modules.crm.api.CrmForm;
import com.bulldozer_collective.modules.crm.api.CrmFormFieldType;
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
public class CrmFormUpdater {

    private final CrmSessionOpener sessionOpener;
    private final CrmGatewayResolver gatewayResolver;
    private final BulldozerLogger logger;

    /**
     * Writes the non-null parts to the CRM, then reads the form back. New {@code fields} replace every field of the
     * form: refused when one of them is a kind the bridge does not cover, since the replacement would drop it.
     */
    public @NonNull CrmForm update(@NonNull UUID customerId,
                                   @NonNull UUID projectId,
                                   @NonNull UUID connectionId,
                                   @NonNull String id,
                                   @NonNull SaveCrmFormBuilder form) throws NoTokenException {
        if (form.isEmpty()) {
            throw new IllegalArgumentException("Nothing to update: every field is null");
        }
        CrmFormValidator.validate(form);
        var session = this.sessionOpener.open(customerId, projectId, connectionId);
        var gateway = this.gatewayResolver.resolve(session);
        if (form.fields() != null && gateway.getForm(session, id).fields().stream()
                .anyMatch(field -> field.fieldType() == CrmFormFieldType.CRM_FORM_FIELD_TYPE_OTHER)) {
            throw new IllegalArgumentException("fields: the form holds a field the bridge does not cover "
                    + "(CRM_FORM_FIELD_TYPE_OTHER), which replacing the fields would drop; edit them in the CRM");
        }
        gateway.updateForm(session, id, form);
        this.logger.info(op("CRM-009", customerId, projectId) + "Updated form %s on connection %s", id, connectionId);
        return gateway.getForm(session, id);
    }
}
