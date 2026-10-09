package com.bulldozer_collective.modules.crm.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.NonNull;

public record SetCrmStageSourceRequest(@NonNull @NotBlank @Size(max = CrmConstraints.SHORT_TEXT) String propertyName) {
}
