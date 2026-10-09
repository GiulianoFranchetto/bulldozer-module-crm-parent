package com.bulldozer_collective.modules.crm.gateway.hubspot.dto;

import org.jspecify.annotations.Nullable;

import java.util.List;

public record HubSpotProperty(@Nullable String name,
                              @Nullable String label,
                              @Nullable String type,
                              @Nullable String fieldType,
                              @Nullable Boolean hidden,
                              @Nullable Boolean archived,
                              @Nullable HubSpotModificationMetadata modificationMetadata,
                              @Nullable List<HubSpotPropertyOption> options) {
}
