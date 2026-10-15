package com.bulldozer_collective.modules.crm.gateway.hubspot.dto;

import org.jspecify.annotations.Nullable;

import java.util.List;

/** A row of a HubSpot form: one or more fields side by side. */
public record HubSpotFormFieldGroup(@Nullable List<HubSpotFormField> fields) {
}
