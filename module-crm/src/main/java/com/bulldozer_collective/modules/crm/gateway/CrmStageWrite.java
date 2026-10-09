package com.bulldozer_collective.modules.crm.gateway;

import org.jspecify.annotations.NonNull;

/** The CRM property, and its value, that put a deal in a given stage. */
public record CrmStageWrite(@NonNull String propertyName,
                            @NonNull String value) {
}
