package com.bulldozer_collective.modules.crm.api;

import java.time.Instant;
import lombok.Builder;
import org.jspecify.annotations.NonNull;

/** The deal property whose value gives the stage of a deal in a pipeline. */
@Builder
public record CrmStageSource(@NonNull String pipelineId,
                             @NonNull String propertyName,
                             @NonNull Instant updatedAt) {
}
