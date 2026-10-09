package com.bulldozer_collective.modules.crm.gateway.hubspot.dto;

import org.jspecify.annotations.Nullable;

import java.util.List;

public record HubSpotPipeline(@Nullable String id,
                              @Nullable String label,
                              @Nullable Integer displayOrder,
                              @Nullable Boolean archived,
                              @Nullable List<HubSpotPipelineStage> stages) {
}
