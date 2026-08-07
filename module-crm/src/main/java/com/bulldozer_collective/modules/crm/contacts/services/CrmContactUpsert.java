package com.bulldozer_collective.modules.crm.contacts.services;

import lombok.Builder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * One contact as the CRM reports it, already mapped out of the platform's own payload. Platform adapters
 * produce these; {@link CrmContactPersister} is the only thing that writes them.
 */
@Builder
public record CrmContactUpsert(@NonNull String sourceId,
                               @Nullable String firstName,
                               @Nullable String lastName,
                               @Nullable String email,
                               @Nullable String phone,
                               @Nullable String jobTitle,
                               @Nullable String linkedInUrl) {
}
