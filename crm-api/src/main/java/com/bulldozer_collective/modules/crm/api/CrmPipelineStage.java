package com.bulldozer_collective.modules.crm.api;

import lombok.Builder;
import org.jspecify.annotations.NonNull;

@Builder
public record CrmPipelineStage(@NonNull String id,
                               @NonNull String label,
                               int displayOrder) {
}
