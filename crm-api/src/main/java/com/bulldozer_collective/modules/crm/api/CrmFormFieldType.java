package com.bulldozer_collective.modules.crm.api;

/**
 * The kinds of form field the bridge reads and writes. {@code CRM_FORM_FIELD_TYPE_OTHER} stands for a field the bridge
 * does not cover (a file upload, a payment link…): read only, refused on a write.
 */
public enum CrmFormFieldType {
    CRM_FORM_FIELD_TYPE_TEXT,
    CRM_FORM_FIELD_TYPE_TEXTAREA,
    CRM_FORM_FIELD_TYPE_EMAIL,
    CRM_FORM_FIELD_TYPE_PHONE,
    CRM_FORM_FIELD_TYPE_NUMBER,
    CRM_FORM_FIELD_TYPE_CHECKBOX,
    CRM_FORM_FIELD_TYPE_SELECT,
    CRM_FORM_FIELD_TYPE_RADIO,
    CRM_FORM_FIELD_TYPE_DATE,
    CRM_FORM_FIELD_TYPE_OTHER
}
