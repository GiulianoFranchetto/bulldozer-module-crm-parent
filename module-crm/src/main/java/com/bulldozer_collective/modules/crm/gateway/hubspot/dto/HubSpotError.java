package com.bulldozer_collective.modules.crm.gateway.hubspot.dto;

import org.jspecify.annotations.Nullable;

/** HubSpot's error body; {@code message} is meant for a human and names what was wrong. */
public record HubSpotError(@Nullable String message,
                           @Nullable String category) {
}
