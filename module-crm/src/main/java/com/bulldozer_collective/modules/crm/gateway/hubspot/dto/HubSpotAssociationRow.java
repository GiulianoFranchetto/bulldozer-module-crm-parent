package com.bulldozer_collective.modules.crm.gateway.hubspot.dto;

import org.jspecify.annotations.Nullable;

import java.util.List;

/** One record of an associations batch read, with the records it is associated with. */
public record HubSpotAssociationRow(@Nullable HubSpotRecordId from,
                                    @Nullable List<HubSpotAssociationTarget> to) {
}
