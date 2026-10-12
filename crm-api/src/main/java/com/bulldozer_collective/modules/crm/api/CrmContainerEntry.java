package com.bulldozer_collective.modules.crm.api;

import lombok.Builder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * One container of the CRM — a HubSpot pipeline, a Salesforce business process — and whether the project imported
 * it. {@code containerId} is set when {@code selected}. {@code existsInCrm} is {@code false} for an imported
 * container the CRM no longer has: delete it. {@code stages} are the pipeline's own stages, in the CRM's order;
 * empty when it no longer exists.
 */
@Builder
public record CrmContainerEntry(@NonNull String remoteId,
                                @NonNull String name,
                                @NonNull CrmContainerType type,
                                boolean selected,
                                @Nullable UUID containerId,
                                boolean existsInCrm,
                                @NonNull List<CrmContainerEntryStage> stages) {
}
