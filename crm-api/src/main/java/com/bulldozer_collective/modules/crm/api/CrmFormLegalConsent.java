package com.bulldozer_collective.modules.crm.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Read and written as is. {@code CRM_FORM_LEGAL_CONSENT_TYPE_LEGITIMATE_INTEREST} requires {@code lawfulBasis} and
 * {@code privacyText}; {@code CRM_FORM_LEGAL_CONSENT_TYPE_NONE} takes neither. The bridge never picks a lawful basis.
 *
 * @param privacyText the notice shown under the form, telling the submitter how their data is used.
 */
public record CrmFormLegalConsent(@NonNull @NotNull CrmFormLegalConsentType type,
                                  @Nullable CrmFormLawfulBasis lawfulBasis,
                                  @Nullable @Size(max = CrmConstraints.LONG_TEXT) String privacyText) {
}
