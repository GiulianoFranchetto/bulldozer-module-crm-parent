package com.bulldozer_collective.modules.crm.security;

import com.bulldozer_collective.logging.BulldozerLogger;
import com.bulldozer_collective.security.Roles;
import com.bulldozer_collective.security.resolvers.RequestContext;
import com.bulldozer_collective.security.services.SecurityTools;
import com.bulldozer_collective.shared.exceptions.ForbiddenException;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

/**
 * Bug 67 — this module was the last one in the fleet whose checker had no ops branch, so a
 * {@code bulldozer-core-ops} operator got a bare 403 on every CRM endpoint of a client project while
 * ads, sea, seo and tracking served the data. These cases pin the four grant conditions.
 * <p>
 * Only the branches that decide before the project-membership lookup are covered: that lookup is a
 * remote call to the projects service, and this is a unit test.
 */
class DefaultAccessCheckerTest {

    private static final UUID CUSTOMER_ID = UUID.randomUUID();
    private static final UUID PROJECT_ID = UUID.randomUUID();

    /**
     * {@code JwtRequestContextResolver} only keeps the {@code ROLE_}-prefixed authorities, so a context
     * built without that prefix matches nothing — the roles below are shaped exactly like the real ones.
     */
    private static final String ROLE_PREFIX = "ROLE_";

    /**
     * The real {@link SecurityTools} over the production role hierarchy, so the test exercises the actual
     * Keycloak role strings and the {@code ROLE_} prefixing rather than a stubbed boolean.
     */
    private final DefaultAccessChecker checker = new DefaultAccessChecker(
            new SecurityTools(RoleHierarchyImpl.withDefaultRolePrefix()
                    .role(Roles.BULLDOZER_ADMINISTRATOR)
                    .implies(Roles.ALL_ROLES)
                    .build()),
            mock(BulldozerLogger.class));

    @Test
    void grantsABulldozerOps() {
        assertDoesNotThrow(() -> this.checker.checkAccess(context(Roles.BULLDOZER_OPS)));
    }

    @Test
    void grantsABulldozerAdministrator() {
        assertDoesNotThrow(() -> this.checker.checkAccess(context(Roles.BULLDOZER_ADMINISTRATOR)));
    }

    @Test
    void grantsACustomerAdministratorOfTheTargetCustomer() {
        var context = RequestContext.builder()
                .userId(UUID.randomUUID())
                .employerCustomerId(CUSTOMER_ID)
                .targetCustomerId(CUSTOMER_ID)
                .projectId(PROJECT_ID)
                .roles(new String[]{ROLE_PREFIX + Roles.BULLDOZER_CUSTOMER_ADMINISTRATOR})
                .firstName("Jordan")
                .email("jordan@example.com")
                .build();

        assertDoesNotThrow(() -> this.checker.checkAccess(context));
    }

    /**
     * The customer-administrator role only counts for the customer the caller is an employee of — holding
     * it while targeting someone else's tenant grants nothing.
     */
    @Test
    void deniesACustomerAdministratorOfAnotherCustomer() {
        var context = RequestContext.builder()
                .employerCustomerId(UUID.randomUUID())
                .targetCustomerId(CUSTOMER_ID)
                .projectId(PROJECT_ID)
                .roles(new String[]{ROLE_PREFIX + Roles.BULLDOZER_CUSTOMER_ADMINISTRATOR})
                .firstName("Jordan")
                .email("jordan@example.com")
                .build();

        assertThrows(ForbiddenException.class, () -> this.checker.checkAccess(context));
    }

    @Test
    void deniesWhenTheCustomerHeaderIsMissing() {
        var context = RequestContext.builder()
                .userId(UUID.randomUUID())
                .projectId(PROJECT_ID)
                .roles(new String[]{ROLE_PREFIX + Roles.BULLDOZER_OPS})
                .firstName("Jordan")
                .email("jordan@example.com")
                .build();

        assertThrows(ForbiddenException.class, () -> this.checker.checkAccess(context));
    }

    @Test
    void deniesWhenTheProjectHeaderIsMissing() {
        var context = RequestContext.builder()
                .userId(UUID.randomUUID())
                .targetCustomerId(CUSTOMER_ID)
                .roles(new String[]{ROLE_PREFIX + Roles.BULLDOZER_OPS})
                .firstName("Jordan")
                .email("jordan@example.com")
                .build();

        assertThrows(ForbiddenException.class, () -> this.checker.checkAccess(context));
    }

    /**
     * No platform role and no user id: the membership lookup cannot even be attempted, and the checker is
     * fail-closed.
     */
    @Test
    void deniesACallerWithNoGrantingRole() {
        var context = RequestContext.builder()
                .targetCustomerId(CUSTOMER_ID)
                .projectId(PROJECT_ID)
                .roles(new String[]{ROLE_PREFIX + Roles.BULLDOZER_EMPLOYEE})
                .firstName("Jordan")
                .email("jordan@example.com")
                .build();

        assertThrows(ForbiddenException.class, () -> this.checker.checkAccess(context));
    }

    private static RequestContext context(String role) {
        return RequestContext.builder()
                .userId(UUID.randomUUID())
                .targetCustomerId(CUSTOMER_ID)
                .projectId(PROJECT_ID)
                .roles(new String[]{ROLE_PREFIX + role})
                .firstName("Jordan")
                .email("jordan@example.com")
                .build();
    }
}
