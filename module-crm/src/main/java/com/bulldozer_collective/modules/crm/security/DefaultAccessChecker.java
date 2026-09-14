package com.bulldozer_collective.modules.crm.security;

import com.bulldozer_collective.logging.BulldozerLogger;
import com.bulldozer_collective.projects.api.clients.InternalGetProjectMembershipClient;
import com.bulldozer_collective.security.Roles;
import com.bulldozer_collective.security.resolvers.RequestContext;
import com.bulldozer_collective.security.services.SecurityTools;
import com.bulldozer_collective.shared.exceptions.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;

import static com.bulldozer_collective.modules.crm.configurations.CrmLogContext.op;

@Service
@RequiredArgsConstructor
public class DefaultAccessChecker {
    private final SecurityTools tools;
    private final BulldozerLogger logger;

    /**
     * Checks if the requester can execute the request.
     * <p>
     * The request can be executed if:
     * <ul>
     *     <li>The requester is a {@link Roles#BULLDOZER_ADMINISTRATOR}</li>
     *     <li>The requester is a {@link Roles#BULLDOZER_OPS}</li>
     *     <li>The requester is a {@link Roles#BULLDOZER_CUSTOMER_ADMINISTRATOR} of the targeted customer</li>
     *     <li>The requester is a member of the targeted project</li>
     * </ul>
     * <p>
     * Both the project id and the customer id must be present in the request context.
     *
     * @param context The request context
     */
    public void checkAccess(RequestContext context) {
        this.logger.debug(op("CRM-006", context.targetCustomerId(), context.projectId())
                + "Checking access for user %s", context.userId());

        if (context.targetCustomerId() == null) {
            this.logger.debug(op("CRM-006") + "Target customer id is missing in the request context");
            throw new ForbiddenException();
        }

        if (context.projectId() == null) {
            this.logger.debug(op("CRM-006") + "Project id is missing in the request context");
            throw new ForbiddenException();
        }

        if (this.tools.hasRole(context, Roles.BULLDOZER_ADMINISTRATOR)) {
            this.logger.debug(op("CRM-006", context.targetCustomerId(), context.projectId())
                    + "Access granted: requester is a Bulldozer administrator");
            return;
        }

        if (this.tools.hasRole(context, Roles.BULLDOZER_OPS)) {
            this.logger.debug(op("CRM-006", context.targetCustomerId(), context.projectId())
                    + "Access granted: requester is a Bulldozer ops");
            return;
        }

        if (this.tools.isTargetCustomerAdmin(context)) {
            this.logger.debug(op("CRM-006", context.targetCustomerId(), context.projectId())
                    + "Access granted: requester is the target customer administrator");
            return;
        }

        try {
            this.logger.debug(op("CRM-006", context.targetCustomerId(), context.projectId())
                    + "Checking project membership");
            var client = InternalGetProjectMembershipClient.builder()
                    .userId(Objects.requireNonNull(context.userId()))
                    .projectId(context.projectId())
                    .customerId(context.targetCustomerId())
                    .build();
            var resp = client.execute();
            if (resp != null && resp.memberships() != null && !resp.memberships().isEmpty()) {
                this.logger.debug(op("CRM-006", context.targetCustomerId(), context.projectId())
                        + "Access granted: requester is a member of the project");
                return;
            }
        } catch (Throwable t) {
            this.logger.error(op("CRM-006", context.targetCustomerId(), context.projectId())
                    + "Failed to check project membership", t);
            throw new ForbiddenException(t);
        }

        this.logger.warn(op("CRM-006", context.targetCustomerId(), context.projectId())
                + "User does not have access to the project");
        throw new ForbiddenException();
    }
}
