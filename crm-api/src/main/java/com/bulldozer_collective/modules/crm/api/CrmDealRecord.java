package com.bulldozer_collective.modules.crm.api;

import com.bulldozer_collective.shared.IntegrationPlatform;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CrmDealRecord(
        @NonNull UUID id,
        @NonNull UUID customerId,
        @NonNull UUID projectId,
        @NonNull IntegrationPlatform platform,
        @NonNull String sourceId,
        @NonNull String name,
        @Nullable BigDecimal amount,
        @Nullable String currency,
        @Nullable Instant closeDate,
        @NonNull CrmDealStage internalStage,
        @NonNull UUID pipelineId,
        @NonNull UUID pipelineStageId,
        @NonNull Instant createdAt
) {}
