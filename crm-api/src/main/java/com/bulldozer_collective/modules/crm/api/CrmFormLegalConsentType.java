package com.bulldozer_collective.modules.crm.api;

/**
 * How a form handles the consent to process the submitter's data. {@code CRM_FORM_LEGAL_CONSENT_TYPE_OTHER} stands for a
 * consent the bridge does not cover (explicit or implicit consent with its checkboxes): read only, refused on a write.
 */
public enum CrmFormLegalConsentType {
    CRM_FORM_LEGAL_CONSENT_TYPE_NONE,
    CRM_FORM_LEGAL_CONSENT_TYPE_LEGITIMATE_INTEREST,
    CRM_FORM_LEGAL_CONSENT_TYPE_OTHER
}
