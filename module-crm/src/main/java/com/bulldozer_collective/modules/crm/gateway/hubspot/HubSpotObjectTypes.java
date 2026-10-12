package com.bulldozer_collective.modules.crm.gateway.hubspot;

import com.bulldozer_collective.modules.crm.api.CrmObjectType;
import org.jspecify.annotations.NonNull;

/**
 * HubSpot's names for the bridge's object types. The objects endpoints are documented with the plural name
 * ({@code /crm/objects/2026-09/contacts}), the associations endpoints and the {@code associations.*} search
 * pseudo-property with the singular one ({@code associations.contact}) — each is used where it is documented.
 */
final class HubSpotObjectTypes {

    private HubSpotObjectTypes() {
    }

    static @NonNull String plural(@NonNull CrmObjectType type) {
        return switch (type) {
            case CRM_OBJECT_TYPE_CONTACT -> "contacts";
            case CRM_OBJECT_TYPE_COMPANY -> "companies";
            case CRM_OBJECT_TYPE_DEAL -> "deals";
            case CRM_OBJECT_TYPE_LEAD -> "leads";
            case CRM_OBJECT_TYPE_CALL -> "calls";
            case CRM_OBJECT_TYPE_EMAIL -> "emails";
            case CRM_OBJECT_TYPE_MEETING -> "meetings";
            case CRM_OBJECT_TYPE_NOTE -> "notes";
            case CRM_OBJECT_TYPE_TASK -> "tasks";
        };
    }

    static @NonNull String singular(@NonNull CrmObjectType type) {
        return switch (type) {
            case CRM_OBJECT_TYPE_CONTACT -> "contact";
            case CRM_OBJECT_TYPE_COMPANY -> "company";
            case CRM_OBJECT_TYPE_DEAL -> "deal";
            case CRM_OBJECT_TYPE_LEAD -> "lead";
            case CRM_OBJECT_TYPE_CALL -> "call";
            case CRM_OBJECT_TYPE_EMAIL -> "email";
            case CRM_OBJECT_TYPE_MEETING -> "meeting";
            case CRM_OBJECT_TYPE_NOTE -> "note";
            case CRM_OBJECT_TYPE_TASK -> "task";
        };
    }
}
