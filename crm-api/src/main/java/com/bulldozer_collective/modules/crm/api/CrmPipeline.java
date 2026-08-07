package com.bulldozer_collective.modules.crm.api;

import com.bulldozer_collective.shared.IntegrationPlatform;

import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.UUID;

public record CrmPipeline(
        @NonNull UUID id,
        @NonNull UUID customerId,
        @NonNull UUID projectId,
        @NonNull IntegrationPlatform platform,
        @NonNull String sourceId,
        @NonNull String name,
        @NonNull List<CrmPipelineStage> stages
) {}
