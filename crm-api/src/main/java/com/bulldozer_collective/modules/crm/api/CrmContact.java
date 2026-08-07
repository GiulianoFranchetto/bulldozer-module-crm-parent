package com.bulldozer_collective.modules.crm.api;

import com.bulldozer_collective.shared.IntegrationPlatform;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

public record CrmContact(
        @NonNull UUID id,
        @NonNull UUID customerId,
        @NonNull UUID projectId,
        @NonNull IntegrationPlatform platform,
        @NonNull String sourceId,
        /** The canonical Bulldozer contact this CRM record resolves to; null while unlinked. */
        @Nullable UUID contactId,
        @Nullable String firstName,
        @Nullable String lastName,
        @Nullable String email,
        @Nullable String phone,
        @Nullable String jobTitle,
        @Nullable String linkedInUrl,
        @NonNull Instant createdAt
) {}
