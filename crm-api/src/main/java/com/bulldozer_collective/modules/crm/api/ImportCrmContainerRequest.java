package com.bulldozer_collective.modules.crm.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Imports the CRM container {@code remoteId}. {@code stagePropertyName} is the enumerated property whose values
 * become the container's stages; absent, the CRM's native stage property is used — set it only when the customer
 * tracks the stage in a custom property.
 */
public record ImportCrmContainerRequest(@NonNull @NotNull CrmContainerType type,
                                        @NonNull @NotBlank @Size(max = CrmConstraints.SHORT_TEXT) String remoteId,
                                        @Nullable @Size(max = CrmConstraints.SHORT_TEXT) String stagePropertyName) {
}
