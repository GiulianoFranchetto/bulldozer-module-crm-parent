package com.bulldozer_collective.modules.crm.forms.services;

import com.bulldozer_collective.modules.crm.api.CrmFormFieldType;
import com.bulldozer_collective.modules.crm.api.CrmObjectType;
import org.jspecify.annotations.NonNull;

import java.util.HashSet;
import java.util.Set;

/**
 * Checks a form before it reaches the CRM, so a mistake answers a {@code 400} naming the field instead of the CRM's
 * own message — or worse, a form written half right.
 */
final class CrmFormValidator {

    private static final Set<CrmObjectType> FIELD_OBJECT_TYPES =
            Set.of(CrmObjectType.CRM_OBJECT_TYPE_CONTACT, CrmObjectType.CRM_OBJECT_TYPE_COMPANY);
    private static final Set<CrmFormFieldType> TYPES_WITH_OPTIONS =
            Set.of(CrmFormFieldType.CRM_FORM_FIELD_TYPE_SELECT, CrmFormFieldType.CRM_FORM_FIELD_TYPE_RADIO);

    private CrmFormValidator() {
    }

    static void validate(@NonNull SaveCrmFormBuilder form) {
        if (form.thankYouMessage() != null && form.redirectUrl() != null) {
            throw new IllegalArgumentException("thankYouMessage, redirectUrl: give one or the other, not both");
        }
        if (form.fields() != null) {
            validateFields(form);
        }
        var consent = form.legalConsent();
        if (consent != null) {
            switch (consent.type()) {
                case CRM_FORM_LEGAL_CONSENT_TYPE_NONE -> {
                    if (consent.lawfulBasis() != null || consent.privacyText() != null) {
                        throw new IllegalArgumentException(
                                "legalConsent: CRM_FORM_LEGAL_CONSENT_TYPE_NONE takes no lawfulBasis nor privacyText");
                    }
                }
                case CRM_FORM_LEGAL_CONSENT_TYPE_LEGITIMATE_INTEREST -> {
                    if (consent.lawfulBasis() == null || consent.privacyText() == null || consent.privacyText().isBlank()) {
                        throw new IllegalArgumentException("legalConsent: CRM_FORM_LEGAL_CONSENT_TYPE_LEGITIMATE_INTEREST "
                                + "needs a lawfulBasis and a privacyText");
                    }
                }
                case CRM_FORM_LEGAL_CONSENT_TYPE_OTHER -> throw new IllegalArgumentException(
                        "legalConsent: CRM_FORM_LEGAL_CONSENT_TYPE_OTHER is read only; set it in the CRM");
            }
        }
    }

    private static void validateFields(@NonNull SaveCrmFormBuilder form) {
        var names = new HashSet<String>();
        for (var field : form.fields()) {
            var where = "fields: '%s'".formatted(field.name());
            if (!FIELD_OBJECT_TYPES.contains(field.objectType())) {
                throw new IllegalArgumentException(where + " writes to a contact or a company only");
            }
            if (field.fieldType() == CrmFormFieldType.CRM_FORM_FIELD_TYPE_OTHER) {
                throw new IllegalArgumentException(where + ": CRM_FORM_FIELD_TYPE_OTHER is read only");
            }
            if (!names.add(field.objectType() + "/" + field.name())) {
                throw new IllegalArgumentException(where + " is given twice");
            }
            var takesOptions = TYPES_WITH_OPTIONS.contains(field.fieldType());
            if (takesOptions && field.options().isEmpty()) {
                throw new IllegalArgumentException(where + ": a " + field.fieldType() + " field needs options");
            }
            if (!takesOptions && !field.options().isEmpty()) {
                throw new IllegalArgumentException(where + ": only a SELECT or RADIO field takes options");
            }
            var values = new HashSet<String>();
            field.options().stream()
                    .filter(option -> !values.add(option.value()))
                    .findFirst()
                    .ifPresent(option -> {
                        throw new IllegalArgumentException(where + ": the option value '%s' is given twice"
                                .formatted(option.value()));
                    });
        }
    }
}
