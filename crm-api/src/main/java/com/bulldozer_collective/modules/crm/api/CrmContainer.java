package com.bulldozer_collective.modules.crm.api;

import lombok.Builder;
import org.jspecify.annotations.NonNull;

import java.time.Instant;
import java.util.UUID;

/**
 * A CRM object that groups deals or leads and their stages — a HubSpot pipeline, a Salesforce sales or lead
 * process — imported into the project. {@code remoteId} is the CRM's own id; {@code stagePropertyName} is the
 * property whose values are the container's stages.
 */
@Builder
public record CrmContainer(@NonNull UUID id,
                           @NonNull CrmContainerType type,
                           @NonNull String remoteId,
                           @NonNull String name,
                           @NonNull String stagePropertyName,
                           @NonNull Instant createdAt,
                           @NonNull Instant updatedAt) {
}
