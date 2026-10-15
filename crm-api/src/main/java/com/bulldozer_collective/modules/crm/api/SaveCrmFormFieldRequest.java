package com.bulldozer_collective.modules.crm.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * @param objectType {@code CRM_OBJECT_TYPE_CONTACT} or {@code CRM_OBJECT_TYPE_COMPANY}.
 * @param name       an existing property of that record type ({@code GET …/properties}).
 * @param options    required by {@code SELECT} and {@code RADIO}, refused by every other type.
 */
public record SaveCrmFormFieldRequest(@NonNull @NotNull CrmObjectType objectType,
                                      @NonNull @NotBlank @Size(max = CrmConstraints.SHORT_TEXT) String name,
                                      @NonNull @NotBlank @Size(max = CrmConstraints.SHORT_TEXT) String label,
                                      @NonNull @NotNull CrmFormFieldType fieldType,
                                      @Nullable Boolean required,
                                      @Nullable @Size(max = 500) List<@NotNull @Valid CrmFormFieldOption> options) {
}
