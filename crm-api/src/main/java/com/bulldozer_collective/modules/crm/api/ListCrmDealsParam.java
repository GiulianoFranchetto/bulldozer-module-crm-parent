package com.bulldozer_collective.modules.crm.api;

import com.bulldozer_collective.shared.IntegrationPlatform;

import org.jspecify.annotations.Nullable;

import java.util.UUID;

public record ListCrmDealsParam(
        @Nullable CrmDealStage internalStage,
        @Nullable UUID pipelineId,
        @Nullable IntegrationPlatform platform,
        int page,
        int size
) {}
