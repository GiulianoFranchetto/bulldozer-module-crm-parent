package com.bulldozer_collective.modules.crm.gateway;

import org.jspecify.annotations.NonNull;

/**
 * The CRM answered in a way the bridge cannot use — a create without an id, a quota that will not reopen in time.
 * Neither the caller's request nor our state is at fault, so it is answered {@code 502}, never {@code 400}.
 */
public class CrmUpstreamException extends RuntimeException {

    public CrmUpstreamException(@NonNull String message) {
        super(message);
    }
}
