package com.bulldozer_collective.modules.crm.api;

import lombok.Builder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * A field of a form: a submission writes its value to the property {@code name} of the record {@code objectType}.
 *
 * @param objectType {@code CRM_OBJECT_TYPE_CONTACT} or {@code CRM_OBJECT_TYPE_COMPANY}; {@code null} for a field writing
 *                   to another kind of record.
 * @param options    the values offered by a {@code SELECT} or {@code RADIO} field, in display order; empty otherwise.
 */
@Builder
public record CrmFormField(@Nullable CrmObjectType objectType,
                           @NonNull String name,
                           @NonNull String label,
                           @NonNull CrmFormFieldType fieldType,
                           boolean required,
                           @NonNull List<CrmFormFieldOption> options) {
}
