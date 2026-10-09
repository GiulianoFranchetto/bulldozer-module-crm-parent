package com.bulldozer_collective.modules.crm.gateway.hubspot.dto;

import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.Map;

/** A CRM record as HubSpot serialises it. Every property value is a string, or {@code null}. */
public record HubSpotObject(@Nullable String id,
                            @Nullable Map<String, String> properties,
                            @Nullable Instant createdAt,
                            @Nullable Instant updatedAt) {
}
