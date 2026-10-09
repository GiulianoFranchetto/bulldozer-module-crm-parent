package com.bulldozer_collective.modules.crm.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.NonNull;

public record CrmAssociationParam(@NonNull @NotNull CrmObjectType fromType,
                                  @NonNull @NotBlank @Size(max = CrmConstraints.SHORT_TEXT) String fromId,
                                  @NonNull @NotNull CrmObjectType toType,
                                  @NonNull @NotBlank @Size(max = CrmConstraints.SHORT_TEXT) String toId) {
}
