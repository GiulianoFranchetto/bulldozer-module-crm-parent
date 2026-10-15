package com.bulldozer_collective.modules.crm.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.NonNull;

/**
 * @param value what is written to a record, and read back from it.
 */
public record CreateCrmPropertyOption(@NonNull @NotBlank @Size(max = CrmConstraints.SHORT_TEXT) String label,
                                      @NonNull @NotBlank @Size(max = CrmConstraints.SHORT_TEXT) String value) {
}
