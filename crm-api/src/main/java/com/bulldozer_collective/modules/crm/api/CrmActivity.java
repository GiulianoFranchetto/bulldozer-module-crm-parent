package com.bulldozer_collective.modules.crm.api;

import com.bulldozer_collective.shared.IntegrationPlatform;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

public record CrmActivity(
        @NonNull UUID id,
        @NonNull UUID customerId,
        @NonNull UUID projectId,
        @NonNull IntegrationPlatform platform,
        @NonNull String sourceId,
        @NonNull CrmActivityType type,
        @Nullable String title,
        @Nullable String body,
        @Nullable Instant dueDate,
        @Nullable Instant completedAt,
        @Nullable UUID dealId,
        @Nullable UUID contactId,
        @Nullable UUID companyId,
        @NonNull Instant createdAt
) {}
