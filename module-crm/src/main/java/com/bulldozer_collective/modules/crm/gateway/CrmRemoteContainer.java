package com.bulldozer_collective.modules.crm.gateway;

import lombok.Builder;
import org.jspecify.annotations.NonNull;

import java.util.List;

/** A container as the CRM serves it — a HubSpot pipeline, a Salesforce business process — with its native stages. */
@Builder
public record CrmRemoteContainer(@NonNull String remoteId,
                                 @NonNull String name,
                                 int displayOrder,
                                 @NonNull List<CrmRemoteContainerStage> stages) {
}
