package com.bulldozer_collective.modules.crm.configurations;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * Builds the standard Bulldozer log prefix for the CRM module. Use via static import and string
 * concatenation ({@code +}, never {@code %s}) so tenant ids are baked into the prefix and never collide
 * with the message's {@code %s} args. Op-ids are registered in {@code BULLDOZER_BACKEND_LOG_DEFINITIONS.md}
 * under {@code # [CRM]}.
 */
public final class CrmLogContext {
    private CrmLogContext() {}

    /** {@code "[CRM-001] [cust=.. proj=..] "} */
    public static String op(@NonNull String operation, @Nullable UUID customerId, @Nullable UUID projectId) {
        return "[" + operation + "] [cust=" + customerId + " proj=" + projectId + "] ";
    }

    /** {@code "[CRM-001] "} — no tenant context */
    public static String op(@NonNull String operation) {
        return "[" + operation + "] ";
    }
}
