package com.bulldozer_collective.modules.crm.gateway;

import org.jspecify.annotations.NonNull;

/** One value a container's stage source property can take in that container, as the CRM labels it. */
public record CrmStageOption(@NonNull String value,
                             @NonNull String label,
                             int displayOrder) {
}
