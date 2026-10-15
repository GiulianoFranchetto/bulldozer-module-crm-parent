package com.bulldozer_collective.modules.crm.gateway.hubspot;

import com.bulldozer_collective.modules.crm.api.CrmForm;
import com.bulldozer_collective.modules.crm.api.CrmFormField;
import com.bulldozer_collective.modules.crm.api.CrmFormFieldOption;
import com.bulldozer_collective.modules.crm.api.CrmFormFieldType;
import com.bulldozer_collective.modules.crm.api.CrmFormLawfulBasis;
import com.bulldozer_collective.modules.crm.api.CrmFormLegalConsent;
import com.bulldozer_collective.modules.crm.api.CrmFormLegalConsentType;
import com.bulldozer_collective.modules.crm.api.CrmObjectType;
import com.bulldozer_collective.modules.crm.forms.services.SaveCrmFormBuilder;
import com.bulldozer_collective.modules.crm.gateway.hubspot.dto.HubSpotForm;
import com.bulldozer_collective.modules.crm.gateway.hubspot.dto.HubSpotFormField;
import com.bulldozer_collective.modules.crm.gateway.hubspot.dto.HubSpotFormLegalConsent;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Translates between HubSpot's forms ({@code /marketing/forms/2027-03-beta}, the only dated version of the forms API,
 * still in beta) and the module's model. Each field of a written form is a row of its own ({@code default_group}).
 */
final class HubSpotFormMapper {

    private static final String CONTACT_TYPE_ID = "0-1";
    private static final String COMPANY_TYPE_ID = "0-2";

    private HubSpotFormMapper() {
    }

    // ── read ─────────────────────────────────────────────────────────────────

    static @NonNull CrmForm form(@NonNull HubSpotForm form) {
        var fields = form.fieldGroups() == null ? List.<CrmFormField>of() : form.fieldGroups().stream()
                .filter(group -> group.fields() != null)
                .flatMap(group -> group.fields().stream())
                .filter(field -> field.name() != null)
                .map(HubSpotFormMapper::field)
                .toList();
        var action = form.configuration() == null ? null : form.configuration().postSubmitAction();
        var actionType = action == null ? null : action.type();
        return CrmForm.builder()
                .id(Objects.requireNonNull(form.id()))
                .name(form.name() == null ? form.id() : form.name())
                .fields(fields)
                .submitButtonText(form.displayOptions() == null ? null : form.displayOptions().submitButtonText())
                .thankYouMessage("thank_you".equals(actionType) ? action.value() : null)
                .redirectUrl("redirect_url".equals(actionType) ? action.value() : null)
                .language(form.configuration() == null ? null : form.configuration().language())
                .legalConsent(legalConsent(form.legalConsentOptions()))
                .createdAt(HubSpotMapper.instant(form.createdAt()))
                .updatedAt(HubSpotMapper.instant(form.updatedAt()))
                .build();
    }

    private static @NonNull CrmFormField field(@NonNull HubSpotFormField field) {
        var options = field.options() == null ? List.<CrmFormFieldOption>of() : field.options().stream()
                .filter(option -> option.value() != null)
                .sorted(Comparator.comparing(option -> option.displayOrder() == null ? 0 : option.displayOrder()))
                .map(option -> new CrmFormFieldOption(
                        option.label() == null ? option.value() : option.label(), option.value()))
                .toList();
        var name = Objects.requireNonNull(field.name());
        return CrmFormField.builder()
                .objectType(objectType(field.objectTypeId()))
                .name(name)
                .label(field.label() == null ? name : field.label())
                .fieldType(fieldType(field.fieldType()))
                .required(Boolean.TRUE.equals(field.required()))
                .options(options)
                .build();
    }

    static @Nullable CrmObjectType objectType(@Nullable String objectTypeId) {
        if (CONTACT_TYPE_ID.equals(objectTypeId)) {
            return CrmObjectType.CRM_OBJECT_TYPE_CONTACT;
        }
        if (COMPANY_TYPE_ID.equals(objectTypeId)) {
            return CrmObjectType.CRM_OBJECT_TYPE_COMPANY;
        }
        return null;
    }

    static @NonNull CrmFormFieldType fieldType(@Nullable String fieldType) {
        return switch (fieldType == null ? "" : fieldType) {
            case "single_line_text" -> CrmFormFieldType.CRM_FORM_FIELD_TYPE_TEXT;
            case "multi_line_text" -> CrmFormFieldType.CRM_FORM_FIELD_TYPE_TEXTAREA;
            case "email" -> CrmFormFieldType.CRM_FORM_FIELD_TYPE_EMAIL;
            case "phone" -> CrmFormFieldType.CRM_FORM_FIELD_TYPE_PHONE;
            case "number" -> CrmFormFieldType.CRM_FORM_FIELD_TYPE_NUMBER;
            case "single_checkbox" -> CrmFormFieldType.CRM_FORM_FIELD_TYPE_CHECKBOX;
            case "dropdown" -> CrmFormFieldType.CRM_FORM_FIELD_TYPE_SELECT;
            case "radio" -> CrmFormFieldType.CRM_FORM_FIELD_TYPE_RADIO;
            case "datepicker" -> CrmFormFieldType.CRM_FORM_FIELD_TYPE_DATE;
            // mobile_phone, multiple_checkboxes, file, payment_link_radio…
            default -> CrmFormFieldType.CRM_FORM_FIELD_TYPE_OTHER;
        };
    }

    static @NonNull CrmFormLegalConsent legalConsent(@Nullable HubSpotFormLegalConsent consent) {
        var type = consent == null || consent.type() == null ? "none" : consent.type();
        return switch (type) {
            case "none" -> new CrmFormLegalConsent(CrmFormLegalConsentType.CRM_FORM_LEGAL_CONSENT_TYPE_NONE, null, null);
            case "legitimate_interest" -> new CrmFormLegalConsent(
                    CrmFormLegalConsentType.CRM_FORM_LEGAL_CONSENT_TYPE_LEGITIMATE_INTEREST,
                    lawfulBasis(consent.lawfulBasis()),
                    consent.privacyText());
            default -> new CrmFormLegalConsent(
                    CrmFormLegalConsentType.CRM_FORM_LEGAL_CONSENT_TYPE_OTHER, null, consent.privacyText());
        };
    }

    private static @Nullable CrmFormLawfulBasis lawfulBasis(@Nullable String lawfulBasis) {
        return switch (lawfulBasis == null ? "" : lawfulBasis) {
            case "lead" -> CrmFormLawfulBasis.CRM_FORM_LAWFUL_BASIS_LEAD;
            case "client" -> CrmFormLawfulBasis.CRM_FORM_LAWFUL_BASIS_CLIENT;
            case "other" -> CrmFormLawfulBasis.CRM_FORM_LAWFUL_BASIS_OTHER;
            default -> null;
        };
    }

    // ── write ────────────────────────────────────────────────────────────────

    /**
     * The body of a creation ({@code create}) or of a partial update: only the parts given. The validation happened
     * upstream ({@code CrmFormValidator}): every field here is writable.
     */
    static @NonNull Map<String, Object> form(@NonNull SaveCrmFormBuilder form, boolean create) {
        var body = new LinkedHashMap<String, Object>();
        if (create) {
            body.put("formType", "hubspot");
            body.put("archived", false);
        }
        if (form.name() != null) {
            body.put("name", form.name());
        }
        if (form.fields() != null) {
            body.put("fieldGroups", form.fields().stream()
                    .map(field -> Map.of(
                            "groupType", "default_group",
                            "richTextType", "text",
                            "fields", List.of(field(field))))
                    .toList());
        }
        var configuration = new LinkedHashMap<String, Object>();
        if (form.language() != null) {
            configuration.put("language", form.language());
        }
        if (form.thankYouMessage() != null) {
            configuration.put("postSubmitAction", Map.of("type", "thank_you", "value", form.thankYouMessage()));
        } else if (form.redirectUrl() != null) {
            configuration.put("postSubmitAction", Map.of("type", "redirect_url", "value", form.redirectUrl()));
        }
        if (!configuration.isEmpty()) {
            body.put("configuration", configuration);
        }
        if (form.submitButtonText() != null) {
            body.put("displayOptions", Map.of("submitButtonText", form.submitButtonText()));
        }
        if (form.legalConsent() != null) {
            body.put("legalConsentOptions", legalConsent(form.legalConsent()));
        }
        return body;
    }

    private static @NonNull Map<String, Object> field(SaveCrmFormBuilder.@NonNull Field field) {
        var body = new LinkedHashMap<String, Object>();
        body.put("objectTypeId", field.objectType() == CrmObjectType.CRM_OBJECT_TYPE_COMPANY
                ? COMPANY_TYPE_ID
                : CONTACT_TYPE_ID);
        body.put("name", field.name());
        body.put("label", field.label());
        body.put("required", field.required());
        body.put("hidden", false);
        body.put("dependentFields", List.of());
        switch (field.fieldType()) {
            case CRM_FORM_FIELD_TYPE_TEXT -> body.put("fieldType", "single_line_text");
            case CRM_FORM_FIELD_TYPE_TEXTAREA -> body.put("fieldType", "multi_line_text");
            case CRM_FORM_FIELD_TYPE_EMAIL -> {
                body.put("fieldType", "email");
                body.put("validation", Map.of("blockedEmailDomains", List.of(), "useDefaultBlockList", false));
            }
            case CRM_FORM_FIELD_TYPE_PHONE -> {
                body.put("fieldType", "phone");
                body.put("useCountryCodeSelect", false);
            }
            case CRM_FORM_FIELD_TYPE_NUMBER -> body.put("fieldType", "number");
            case CRM_FORM_FIELD_TYPE_CHECKBOX -> body.put("fieldType", "single_checkbox");
            case CRM_FORM_FIELD_TYPE_SELECT -> putChoices(body, "dropdown", field.options());
            case CRM_FORM_FIELD_TYPE_RADIO -> putChoices(body, "radio", field.options());
            case CRM_FORM_FIELD_TYPE_DATE -> body.put("fieldType", "datepicker");
            case CRM_FORM_FIELD_TYPE_OTHER -> throw new IllegalArgumentException(
                    "fields: '%s' is CRM_FORM_FIELD_TYPE_OTHER, which cannot be written".formatted(field.name()));
        }
        return body;
    }

    private static void putChoices(@NonNull Map<String, Object> body,
                                   @NonNull String fieldType,
                                   @NonNull List<CrmFormFieldOption> options) {
        body.put("fieldType", fieldType);
        body.put("defaultValues", List.of());
        var choices = new ArrayList<Map<String, Object>>();
        for (var i = 0; i < options.size(); i++) {
            choices.add(Map.of("label", options.get(i).label(), "value", options.get(i).value(), "displayOrder", i));
        }
        body.put("options", choices);
    }

    private static @NonNull Map<String, Object> legalConsent(@NonNull CrmFormLegalConsent consent) {
        return switch (consent.type()) {
            case CRM_FORM_LEGAL_CONSENT_TYPE_NONE -> Map.of("type", "none");
            case CRM_FORM_LEGAL_CONSENT_TYPE_LEGITIMATE_INTEREST -> Map.of(
                    "type", "legitimate_interest",
                    "lawfulBasis", switch (Objects.requireNonNull(consent.lawfulBasis())) {
                        case CRM_FORM_LAWFUL_BASIS_LEAD -> "lead";
                        case CRM_FORM_LAWFUL_BASIS_CLIENT -> "client";
                        case CRM_FORM_LAWFUL_BASIS_OTHER -> "other";
                    },
                    "privacyText", Objects.requireNonNull(consent.privacyText()),
                    "subscriptionTypeIds", List.of());
            case CRM_FORM_LEGAL_CONSENT_TYPE_OTHER -> throw new IllegalArgumentException(
                    "legalConsent: CRM_FORM_LEGAL_CONSENT_TYPE_OTHER cannot be written");
        };
    }
}
