package com.bulldozer_collective.modules.crm.gateway.hubspot.dto;

import org.jspecify.annotations.Nullable;

public record HubSpotPipelineStage(@Nullable String id,
                                   @Nullable String label,
                                   @Nullable Integer displayOrder,
                                   @Nullable Boolean archived) {
}
