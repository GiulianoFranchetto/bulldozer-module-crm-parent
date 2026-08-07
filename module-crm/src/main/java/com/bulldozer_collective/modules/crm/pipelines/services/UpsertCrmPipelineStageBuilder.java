package com.bulldozer_collective.modules.crm.pipelines.services;

import lombok.Builder;
import org.jspecify.annotations.NonNull;

import java.util.UUID;

@Builder
public record UpsertCrmPipelineStageBuilder(@NonNull UUID customerId,
                                            @NonNull UUID projectId,
                                            @NonNull UUID pipelineId,
                                            @NonNull String sourceId,
                                            @NonNull String label,
                                            int displayOrder) {
}
