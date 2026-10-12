package com.bulldozer_collective.modules.crm.gateway;

import lombok.Builder;
import org.jspecify.annotations.NonNull;

@Builder
public record CrmRemoteContainerStage(@NonNull String remoteId,
                                      @NonNull String name,
                                      int displayOrder) {
}
