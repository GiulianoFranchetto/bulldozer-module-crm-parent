package com.bulldozer_collective.modules.crm.sync.services.hubspot.dto;

import org.jspecify.annotations.Nullable;

public record HubSpotPaging(
        @Nullable HubSpotPagingNext next
) {}
