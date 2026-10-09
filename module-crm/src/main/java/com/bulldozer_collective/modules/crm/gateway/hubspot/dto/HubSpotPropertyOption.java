package com.bulldozer_collective.modules.crm.gateway.hubspot.dto;

import org.jspecify.annotations.Nullable;

public record HubSpotPropertyOption(@Nullable String label,
                                    @Nullable String value,
                                    @Nullable Integer displayOrder,
                                    @Nullable Boolean hidden) {
}
