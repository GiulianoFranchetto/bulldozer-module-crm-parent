package com.bulldozer_collective.modules.crm.api;

import com.bulldozer_collective.shared.IntegrationPlatform;

import org.jspecify.annotations.Nullable;

import java.util.UUID;

public record ListCrmActivitiesParam(
        @Nullable CrmActivityType type,
        @Nullable UUID dealId,
        @Nullable UUID contactId,
        @Nullable UUID companyId,
        @Nullable IntegrationPlatform platform,
        int page,
        int size
) {}
