package com.bulldozer_collective.modules.crm.api;

/**
 * The kinds of property the bridge can create in the CRM: a single-line or multi-line text, a number, a date, a
 * checkbox, or a dropdown of fixed options.
 */
public enum CrmPropertyFieldType {
    CRM_PROPERTY_FIELD_TYPE_TEXT,
    CRM_PROPERTY_FIELD_TYPE_TEXTAREA,
    CRM_PROPERTY_FIELD_TYPE_NUMBER,
    CRM_PROPERTY_FIELD_TYPE_DATE,
    CRM_PROPERTY_FIELD_TYPE_CHECKBOX,
    CRM_PROPERTY_FIELD_TYPE_SELECT
}
