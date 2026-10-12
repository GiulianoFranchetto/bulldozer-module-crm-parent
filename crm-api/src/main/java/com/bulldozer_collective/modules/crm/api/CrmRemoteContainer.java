package com.bulldozer_collective.modules.crm.api;

import lombok.Builder;
import org.jspecify.annotations.NonNull;

import java.util.List;

/** A pipeline as the CRM serves it, before import, with its native stages. */
@Builder
public record CrmRemoteContainer(@NonNull String remoteId,
                                 @NonNull String name,
                                 int displayOrder,
                                 @NonNull List<CrmRemoteContainerStage> stages) {
}
