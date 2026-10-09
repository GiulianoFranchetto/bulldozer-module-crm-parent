package com.bulldozer_collective.modules.crm.gateway;

import org.jspecify.annotations.NonNull;

/**
 * The CRM refused the project's token ({@code 401} / {@code 403}): it was revoked, or lacks a scope. The caller
 * fixes it by reconnecting the CRM, so the message must reach them.
 */
public class CrmAuthorizationException extends RuntimeException {

    public CrmAuthorizationException(@NonNull String message) {
        super(message);
    }
}
