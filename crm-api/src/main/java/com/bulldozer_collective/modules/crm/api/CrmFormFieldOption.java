package com.bulldozer_collective.modules.crm.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.NonNull;

/** @param value what the submission writes to the CRM property. */
public record CrmFormFieldOption(@NonNull @NotBlank @Size(max = CrmConstraints.SHORT_TEXT) String label,
                                 @NonNull @NotBlank @Size(max = CrmConstraints.SHORT_TEXT) String value) {
}
