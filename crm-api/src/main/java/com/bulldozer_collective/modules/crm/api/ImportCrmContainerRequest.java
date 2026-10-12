package com.bulldozer_collective.modules.crm.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.NonNull;

/**
 * Imports the CRM pipeline {@code remoteId} as a container. {@code stagePropertyName} is the enumerated property
 * whose values become the container's stages — the CRM's native stage property, or a custom one.
 */
public record ImportCrmContainerRequest(@NonNull @NotNull CrmContainerType type,
                                        @NonNull @NotBlank @Size(max = CrmConstraints.SHORT_TEXT) String remoteId,
                                        @NonNull @NotBlank @Size(max = CrmConstraints.SHORT_TEXT) String stagePropertyName) {
}
