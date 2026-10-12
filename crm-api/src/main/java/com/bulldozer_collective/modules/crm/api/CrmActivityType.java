package com.bulldozer_collective.modules.crm.api;

import org.jspecify.annotations.NonNull;

public enum CrmActivityType {
    CRM_ACTIVITY_TYPE_CALL,
    CRM_ACTIVITY_TYPE_EMAIL,
    CRM_ACTIVITY_TYPE_MEETING,
    CRM_ACTIVITY_TYPE_NOTE,
    CRM_ACTIVITY_TYPE_TASK;

    /** The CRM object an activity of this type is: each activity type is its own object in the CRM. */
    public @NonNull CrmObjectType objectType() {
        return switch (this) {
            case CRM_ACTIVITY_TYPE_CALL -> CrmObjectType.CRM_OBJECT_TYPE_CALL;
            case CRM_ACTIVITY_TYPE_EMAIL -> CrmObjectType.CRM_OBJECT_TYPE_EMAIL;
            case CRM_ACTIVITY_TYPE_MEETING -> CrmObjectType.CRM_OBJECT_TYPE_MEETING;
            case CRM_ACTIVITY_TYPE_NOTE -> CrmObjectType.CRM_OBJECT_TYPE_NOTE;
            case CRM_ACTIVITY_TYPE_TASK -> CrmObjectType.CRM_OBJECT_TYPE_TASK;
        };
    }
}
