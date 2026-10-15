package com.bulldozer_collective.modules.crm.gateway.hubspot.dto;

import org.jspecify.annotations.Nullable;

import java.util.List;

/** @param objectTypeId the record type the field writes to: {@code 0-1} a contact, {@code 0-2} a company. */
public record HubSpotFormField(@Nullable String objectTypeId,
                               @Nullable String name,
                               @Nullable String label,
                               @Nullable String fieldType,
                               @Nullable Boolean required,
                               @Nullable List<HubSpotPropertyOption> options) {
}
