package com.bulldozer_collective.modules.crm.gateway.hubspot;

import com.bulldozer_collective.modules.crm.api.CrmContainerType;
import com.bulldozer_collective.modules.crm.api.CrmObjectType;
import org.jspecify.annotations.NonNull;

/**
 * How HubSpot stores each kind of container.
 *
 * <p>Deals: pipelines under object type id {@code 0-3} (pipelines guide), membership in {@code pipeline}, stage in
 * {@code dealstage}. Leads: object type id {@code 0-136} (Object APIs guide); HubSpot's official docs do not name
 * the leads' pipeline and stage properties — {@code hs_pipeline} / {@code hs_pipeline_stage} follow the convention
 * the docs give for tickets, projects and services, and are to be confirmed against
 * {@code GET /crm/properties/2026-09/leads} on a real portal.
 *
 * @param pipelineTypeId     the object type id the pipelines endpoint is documented with.
 * @param objectType         the records grouped by the pipeline.
 * @param propertiesPath     the object name of the properties endpoint.
 * @param pipelineProperty   the property holding a record's pipeline id.
 * @param nativeStageProperty the property holding a record's native stage id, specific to its pipeline.
 */
record HubSpotContainerKind(@NonNull String pipelineTypeId,
                            @NonNull CrmObjectType objectType,
                            @NonNull String propertiesPath,
                            @NonNull String pipelineProperty,
                            @NonNull String nativeStageProperty) {

    static final HubSpotContainerKind DEALS =
            new HubSpotContainerKind("0-3", CrmObjectType.CRM_OBJECT_TYPE_DEAL, "deals", "pipeline", "dealstage");
    static final HubSpotContainerKind LEADS =
            new HubSpotContainerKind("0-136", CrmObjectType.CRM_OBJECT_TYPE_LEAD, "leads", "hs_pipeline",
                    "hs_pipeline_stage");

    static @NonNull HubSpotContainerKind of(@NonNull CrmContainerType type) {
        return switch (type) {
            case CRM_CONTAINER_TYPE_DEAL -> DEALS;
            case CRM_CONTAINER_TYPE_LEAD -> LEADS;
        };
    }
}
