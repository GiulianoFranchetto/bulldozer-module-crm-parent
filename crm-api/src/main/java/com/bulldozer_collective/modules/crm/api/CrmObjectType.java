package com.bulldozer_collective.modules.crm.api;

/**
 * The CRM objects the bridge can read, write and associate. Activities are split by type, because every CRM
 * stores each kind of activity as its own object.
 */
public enum CrmObjectType {
    CRM_OBJECT_TYPE_CONTACT,
    CRM_OBJECT_TYPE_COMPANY,
    CRM_OBJECT_TYPE_DEAL,
    CRM_OBJECT_TYPE_CALL,
    CRM_OBJECT_TYPE_EMAIL,
    CRM_OBJECT_TYPE_MEETING,
    CRM_OBJECT_TYPE_NOTE,
    CRM_OBJECT_TYPE_TASK
}
