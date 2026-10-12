package com.bulldozer_collective.modules.crm.activities.services;

import com.bulldozer_collective.modules.crm.api.CrmActivity;
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
public class CrmActivityFactory {

    private final CrmSessionOpener sessionOpener;
    private final CrmGatewayResolver gatewayResolver;
    private final BulldozerLogger logger;

    /**
     * Logs the activity in the CRM, attaches it to each record of {@code associations}, then reads it back.
     *
     * <p>Not atomic: the CRM has no transaction spanning a create and its associations. If an association fails,
     * the activity stays in the CRM unattached to the remaining records — the error names it so the caller can
     * retry the association alone.
     */
    public @NonNull CrmActivity create(@NonNull UUID customerId,
                                       @NonNull UUID projectId,
                                       @NonNull UUID connectionId,
                                       @NonNull CreateCrmActivityBuilder activity) throws NoTokenException {
        var session = this.sessionOpener.open(customerId, projectId, connectionId);
        var gateway = this.gatewayResolver.resolve(session);
        var id = gateway.createActivity(session, activity);
        this.logger.info(op("CRM-004", customerId, projectId) + "Created %s %s on connection %s",
                activity.type(), id, connectionId);

        var self = new CrmObjectRef(activity.type().objectType(), id);
        for (var target : activity.associations()) {
            try {
                gateway.associate(session, self, target);
            } catch (RuntimeException e) {
                this.logger.warn(op("CRM-004", customerId, projectId)
                        + "Activity %s was created but could not be associated with %s %s",
                        id, target.type(), target.id(), e);
                throw e;
            }
        }
        return gateway.getActivity(session, activity.type(), id);
    }
}
