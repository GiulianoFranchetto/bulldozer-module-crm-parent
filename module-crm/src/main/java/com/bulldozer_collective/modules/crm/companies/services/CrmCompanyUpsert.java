package com.bulldozer_collective.modules.crm.companies.services;

import lombok.Builder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * One company as the CRM reports it, already mapped out of the platform's own payload.
 */
@Builder
public record CrmCompanyUpsert(@NonNull String sourceId,
                               @NonNull String name,
                               @Nullable String domain,
                               @Nullable String industry,
                               @Nullable String country,
                               @Nullable String city) {
}
