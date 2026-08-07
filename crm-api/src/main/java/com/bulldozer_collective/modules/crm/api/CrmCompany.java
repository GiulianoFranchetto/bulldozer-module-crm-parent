package com.bulldozer_collective.modules.crm.api;

import com.bulldozer_collective.shared.IntegrationPlatform;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

public record CrmCompany(
        @NonNull UUID id,
        @NonNull UUID customerId,
        @NonNull UUID projectId,
        @NonNull IntegrationPlatform platform,
        @NonNull String sourceId,
        /** The canonical Bulldozer company this CRM record resolves to; null while unlinked. */
        @Nullable UUID companyId,
        @NonNull String name,
        @Nullable String domain,
        @Nullable String industry,
        @Nullable String country,
        @Nullable String city,
        @NonNull Instant createdAt
) {}
