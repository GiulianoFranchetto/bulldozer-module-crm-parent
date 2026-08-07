package com.bulldozer_collective.modules.crm.api;

import com.bulldozer_collective.shared.IntegrationPlatform;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

public record CrmSyncRun(
        @NonNull UUID id,
        @NonNull UUID customerId,
        @NonNull UUID projectId,
        @NonNull IntegrationPlatform platform,
        @NonNull Instant startedAt,
        @Nullable Instant finishedAt,
        @NonNull CrmSyncStatus status,
        int contactsSynced,
        int companiesSynced,
        int dealsSynced,
        int activitiesSynced,
        @Nullable String errorMessage
) {}
