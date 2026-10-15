package com.bulldozer_collective.modules.crm.containers.services;

import com.bulldozer_collective.modules.crm.api.CrmProperty;
import com.bulldozer_collective.modules.crm.api.CrmPropertyFieldType;
import com.bulldozer_collective.modules.crm.gateway.CrmGatewayResolver;
import com.bulldozer_collective.modules.crm.gateway.CrmSessionOpener;
import com.bulldozer_collective.modules.crm.gateway.NoTokenException;
import com.bulldozer_collective.logging.BulldozerLogger;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.UUID;

import static com.bulldozer_collective.modules.crm.configurations.CrmLogContext.op;

@Service
@RequiredArgsConstructor
public class CrmPropertyFactory {

    private final CrmSessionOpener sessionOpener;
    private final CrmGatewayResolver gatewayResolver;
    private final BulldozerLogger logger;

    /**
     * Creates the property in the CRM, on every record of its type. Nothing is stored: a name already taken is
     * refused by the CRM itself (409).
     */
    public @NonNull CrmProperty create(@NonNull UUID customerId,
                                       @NonNull UUID projectId,
                                       @NonNull UUID connectionId,
                                       @NonNull CreateCrmPropertyBuilder property) throws NoTokenException {
        validateOptions(property);
        var session = this.sessionOpener.open(customerId, projectId, connectionId);
        var created = this.gatewayResolver.resolve(session).createProperty(session, property);
        this.logger.info(op("CRM-007", customerId, projectId) + "Created %s property %s on connection %s",
                property.objectType(), created.name(), connectionId);
        return created;
    }

    private static void validateOptions(@NonNull CreateCrmPropertyBuilder property) {
        var select = property.fieldType() == CrmPropertyFieldType.CRM_PROPERTY_FIELD_TYPE_SELECT;
        if (select && property.options().isEmpty()) {
            throw new IllegalArgumentException("options: a CRM_PROPERTY_FIELD_TYPE_SELECT property needs at least one");
        }
        if (!select && !property.options().isEmpty()) {
            throw new IllegalArgumentException("options: only a CRM_PROPERTY_FIELD_TYPE_SELECT property takes options");
        }
        var values = new HashSet<String>();
        property.options().stream()
                .filter(option -> !values.add(option.value()))
                .findFirst()
                .ifPresent(option -> {
                    throw new IllegalArgumentException("options: the value '%s' is given twice".formatted(option.value()));
                });
    }
}
