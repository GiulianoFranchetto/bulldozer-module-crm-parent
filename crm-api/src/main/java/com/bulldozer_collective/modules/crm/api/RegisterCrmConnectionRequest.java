package com.bulldozer_collective.modules.crm.api;

import com.bulldozer_collective.shared.IntegrationPlatform;

import org.jspecify.annotations.NonNull;

public record RegisterCrmConnectionRequest(
        @NonNull IntegrationPlatform platform
) {}
