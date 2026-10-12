package com.bulldozer_collective.modules.crm.containers.services;

import com.bulldozer_collective.modules.crm.api.CrmContainerType;
import lombok.Builder;
import org.jspecify.annotations.NonNull;

@Builder
public record ImportCrmContainerBuilder(@NonNull CrmContainerType type,
                                        @NonNull String remoteId,
                                        @NonNull String stagePropertyName) {
}
