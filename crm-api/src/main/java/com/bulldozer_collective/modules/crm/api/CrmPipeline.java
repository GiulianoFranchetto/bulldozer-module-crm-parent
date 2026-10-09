package com.bulldozer_collective.modules.crm.api;

import java.util.List;
import lombok.Builder;
import org.jspecify.annotations.NonNull;

/** A deal pipeline of the CRM, with its native stages. */
@Builder
public record CrmPipeline(@NonNull String id,
                          @NonNull String label,
                          int displayOrder,
                          @NonNull List<CrmPipelineStage> stages) {
}
