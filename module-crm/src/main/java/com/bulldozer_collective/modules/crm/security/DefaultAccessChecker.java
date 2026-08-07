package com.bulldozer_collective.modules.crm.security;

import com.bulldozer_collective.logging.BulldozerLogger;
import com.bulldozer_collective.projects.api.clients.GetProjectMembershipClient;
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

    public void checkAccess(RequestContext context) {
        if (context.targetCustomerId() == null) {
            this.logger.debug(op("CRM-006") + "Target customer id is missing in the request context");
            throw new ForbiddenException();
        }

        if (context.projectId() == null) {
            this.logger.debug(op("CRM-006") + "Project id is missing in the request context");
            throw new ForbiddenException();
        }

        if (this.tools.hasRole(context, Roles.BULLDOZER_ADMINISTRATOR)) {
            return;
        }

        if (this.tools.isTargetCustomerAdmin(context)) {
            return;
        }

        try {
            this.logger.debug(op("CRM-006", context.targetCustomerId(), context.projectId())
                    + "Checking project membership");
            var client = GetProjectMembershipClient.builder()
                    .userId(Objects.requireNonNull(context.userId()))
                    .projectId(context.projectId())
                    .customerId(context.targetCustomerId())
                    .auth(context.authorizationHeader())
                    .build();
            var resp = client.execute();
            if (resp != null && resp.memberships() != null && !resp.memberships().isEmpty()) {
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
