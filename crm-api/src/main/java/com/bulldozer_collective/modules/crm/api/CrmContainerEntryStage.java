package com.bulldozer_collective.modules.crm.api;

import lombok.Builder;
import org.jspecify.annotations.NonNull;

/**
 * One of the pipeline's own stages, as the CRM defines it — what {@code pipelineStage} takes when a deal or a lead
 * is created in a container that reads its stages from a custom property.
 */
@Builder
public record CrmContainerEntryStage(@NonNull String remoteId,
                                     @NonNull String name,
                                     int displayOrder) {
}
