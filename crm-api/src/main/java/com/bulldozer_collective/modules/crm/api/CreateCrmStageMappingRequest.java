package com.bulldozer_collective.modules.crm.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.NonNull;

public record CreateCrmStageMappingRequest(@NonNull @NotBlank @Size(max = CrmConstraints.SHORT_TEXT) String value,
                                           @NonNull @NotNull CrmDealStage stage,
                                           boolean writeDefault) {
}
