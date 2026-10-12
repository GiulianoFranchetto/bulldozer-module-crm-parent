package com.bulldozer_collective.modules.crm.containers.services;

import com.bulldozer_collective.modules.crm.api.CrmContainerType;
import lombok.Builder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** {@code stagePropertyName} {@code null} → the CRM's native stage property. */
@Builder
public record ImportCrmContainerBuilder(@NonNull CrmContainerType type,
                                        @NonNull String remoteId,
                                        @Nullable String stagePropertyName) {
}
