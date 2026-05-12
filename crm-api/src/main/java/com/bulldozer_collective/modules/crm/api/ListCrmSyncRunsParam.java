package com.bulldozer_collective.modules.crm.api;

import com.bulldozer_collective.shared.IntegrationPlatform;

import org.jspecify.annotations.Nullable;

public record ListCrmSyncRunsParam(
        @Nullable IntegrationPlatform platform,
        @Nullable CrmSyncStatus status,
        int page,
        int size
) {}
