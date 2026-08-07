package com.bulldozer_collective.modules.crm.pipelines.services;

import com.bulldozer_collective.shared.IntegrationPlatform;
import lombok.Builder;
import org.jspecify.annotations.NonNull;

import java.util.UUID;

@Builder
public record UpsertCrmPipelineBuilder(@NonNull UUID customerId,
                                       @NonNull UUID projectId,
                                       @NonNull IntegrationPlatform platform,
                                       @NonNull String sourceId,
                                       @NonNull String name) {
}
