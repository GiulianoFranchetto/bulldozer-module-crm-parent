package com.bulldozer_collective.modules.crm.gateway;

import org.jspecify.annotations.NonNull;

/** An association to create between two CRM records, by their CRM ids. */
public record CrmLink(@NonNull String fromId, @NonNull String toId) {
}
