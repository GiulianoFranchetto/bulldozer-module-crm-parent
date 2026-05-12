package com.bulldozer_collective.modules.crm.api;

import com.bulldozer_collective.shared.IntegrationPlatform;

import org.jspecify.annotations.Nullable;

public record ListCrmContactsParam(
        @Nullable String q,
        @Nullable IntegrationPlatform platform,
        int page,
        int size
) {}
