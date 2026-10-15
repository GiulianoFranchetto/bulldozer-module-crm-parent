package com.bulldozer_collective.modules.crm.gateway.hubspot.dto;

import org.jspecify.annotations.Nullable;

import java.util.List;

/** A form definition of HubSpot's forms API. Dates are ISO-8601 strings. */
public record HubSpotForm(@Nullable String id,
                          @Nullable String name,
                          @Nullable String createdAt,
                          @Nullable String updatedAt,
                          @Nullable List<HubSpotFormFieldGroup> fieldGroups,
                          @Nullable HubSpotFormConfiguration configuration,
                          @Nullable HubSpotFormDisplayOptions displayOptions,
                          @Nullable HubSpotFormLegalConsent legalConsentOptions) {
}
