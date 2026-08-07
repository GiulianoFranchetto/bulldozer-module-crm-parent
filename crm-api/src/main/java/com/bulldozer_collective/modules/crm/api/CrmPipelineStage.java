package com.bulldozer_collective.modules.crm.api;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

public record CrmPipelineStage(
        @NonNull UUID id,
        @NonNull String sourceId,
        @NonNull String label,
        int displayOrder,
        @Nullable CrmDealStage mappedInternalStage,
        @Nullable UUID mappingId
) {}
