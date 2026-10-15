package com.bulldozer_collective.modules.crm.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * A property to create in the CRM, on every record of {@code objectType}.
 *
 * @param name      the property's internal name, used to read and write it: lowercase snake case.
 * @param groupName the CRM's property group to file it under — an existing one, as {@code groupName} of
 *                  {@code GET …/properties} shows (HubSpot: {@code contactinformation} for a contact).
 * @param options   the dropdown's values: required by {@code CRM_PROPERTY_FIELD_TYPE_SELECT}, refused otherwise.
 */
public record CreateCrmPropertyRequest(@NonNull @NotNull CrmObjectType objectType,
                                       @NonNull @NotBlank @Size(max = CrmConstraints.SHORT_TEXT)
                                       @Pattern(regexp = "[a-z][a-z0-9_]*",
                                               message = "must be lowercase snake case, starting with a letter")
                                       String name,
                                       @NonNull @NotBlank @Size(max = CrmConstraints.SHORT_TEXT) String label,
                                       @NonNull @NotBlank @Size(max = CrmConstraints.SHORT_TEXT) String groupName,
                                       @Nullable @Size(max = CrmConstraints.SHORT_TEXT) String description,
                                       @NonNull @NotNull CrmPropertyFieldType fieldType,
                                       @Nullable @Size(max = 500) List<@NotNull @Valid CreateCrmPropertyOption> options) {
}
