package com.bulldozer_collective.modules.crm.gateway.hubspot.dto;

import org.jspecify.annotations.Nullable;

import java.util.List;

/** HubSpot's envelope for every listing and search. */
public record HubSpotResults<T>(@Nullable List<T> results,
                                @Nullable HubSpotPaging paging) {
}
