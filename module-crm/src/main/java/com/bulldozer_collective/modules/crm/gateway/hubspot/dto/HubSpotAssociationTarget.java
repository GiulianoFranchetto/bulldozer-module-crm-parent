package com.bulldozer_collective.modules.crm.gateway.hubspot.dto;

import org.jspecify.annotations.Nullable;

/** {@code toObjectId} is a number in HubSpot's answer. */
public record HubSpotAssociationTarget(@Nullable Long toObjectId) {
}
