package com.bulldozer_collective.modules.crm.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * @param fields          the form's fields, in display order.
 * @param thankYouMessage shown after a submission; exclusive with {@code redirectUrl}. Both absent: the CRM's default.
 * @param legalConsent    {@code CRM_FORM_LEGAL_CONSENT_TYPE_NONE} when absent.
 */
public record CreateCrmFormRequest(@NonNull @NotBlank @Size(max = CrmConstraints.SHORT_TEXT) String name,
                                   @NonNull @NotEmpty @Size(max = CrmConstraints.MAX_FORM_FIELDS)
                                   List<@NotNull @Valid SaveCrmFormFieldRequest> fields,
                                   @Nullable @Size(max = CrmConstraints.SHORT_TEXT) String submitButtonText,
                                   @Nullable @Size(max = CrmConstraints.LONG_TEXT) String thankYouMessage,
                                   @Nullable @Size(max = CrmConstraints.URL) String redirectUrl,
                                   @Nullable @Size(max = CrmConstraints.LANGUAGE) String language,
                                   @Nullable @Valid CrmFormLegalConsent legalConsent) {
}
