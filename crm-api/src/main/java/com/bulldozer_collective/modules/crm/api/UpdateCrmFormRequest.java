package com.bulldozer_collective.modules.crm.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Only the non-null fields are written; at least one is required. {@code fields} replaces every field of the form —
 * refused when the form holds a field of type {@code CRM_FORM_FIELD_TYPE_OTHER}, which the replacement would drop.
 */
public record UpdateCrmFormRequest(@Nullable @Size(max = CrmConstraints.SHORT_TEXT) String name,
                                   @Nullable @Size(min = 1, max = CrmConstraints.MAX_FORM_FIELDS)
                                   List<@NotNull @Valid SaveCrmFormFieldRequest> fields,
                                   @Nullable @Size(max = CrmConstraints.SHORT_TEXT) String submitButtonText,
                                   @Nullable @Size(max = CrmConstraints.LONG_TEXT) String thankYouMessage,
                                   @Nullable @Size(max = CrmConstraints.URL) String redirectUrl,
                                   @Nullable @Size(max = CrmConstraints.LANGUAGE) String language,
                                   @Nullable @Valid CrmFormLegalConsent legalConsent) {
}
