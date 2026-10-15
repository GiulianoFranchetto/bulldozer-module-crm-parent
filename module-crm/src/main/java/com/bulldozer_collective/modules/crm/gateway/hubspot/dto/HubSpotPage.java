package com.bulldozer_collective.modules.crm.gateway.hubspot.dto;

import org.jspecify.annotations.Nullable;

import java.util.Map;

/** A landing page of HubSpot's pages API. Dates are ISO-8601 strings. */
public record HubSpotPage(@Nullable String id,
                          @Nullable String name,
                          @Nullable String slug,
                          @Nullable String domain,
                          @Nullable String url,
                          @Nullable String htmlTitle,
                          @Nullable String metaDescription,
                          @Nullable String language,
                          @Nullable String templatePath,
                          @Nullable String currentState,
                          @Nullable Boolean currentlyPublished,
                          @Nullable String publishDate,
                          @Nullable String created,
                          @Nullable String updated,
                          @Nullable Map<String, Object> layoutSections,
                          @Nullable Map<String, Object> widgets) {
}
