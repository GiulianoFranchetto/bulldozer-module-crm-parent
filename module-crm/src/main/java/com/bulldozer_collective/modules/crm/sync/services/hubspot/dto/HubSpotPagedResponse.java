package com.bulldozer_collective.modules.crm.sync.services.hubspot.dto;

import org.jspecify.annotations.Nullable;

import java.util.List;

public record HubSpotPagedResponse<T>(
        List<T> results,
        @Nullable HubSpotPaging paging
) {}
