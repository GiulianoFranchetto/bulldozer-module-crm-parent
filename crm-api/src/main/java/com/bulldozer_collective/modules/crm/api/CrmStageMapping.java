package com.bulldozer_collective.modules.crm.api;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import org.jspecify.annotations.NonNull;

/**
 * "In this pipeline, the stage source value {@code value} means {@code stage}." When several values map to the
 * same stage, the one marked {@code writeDefault} is the value written when a deal is set to that stage.
 */
@Builder
public record CrmStageMapping(@NonNull UUID id,
                              @NonNull String pipelineId,
                              @NonNull String value,
                              @NonNull CrmDealStage stage,
                              boolean writeDefault,
                              @NonNull Instant createdAt) {
}
