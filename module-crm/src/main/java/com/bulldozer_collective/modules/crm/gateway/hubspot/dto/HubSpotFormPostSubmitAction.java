package com.bulldozer_collective.modules.crm.gateway.hubspot.dto;

import org.jspecify.annotations.Nullable;

/** @param type {@code thank_you} ({@code value} is the message) or {@code redirect_url} ({@code value} is the URL). */
public record HubSpotFormPostSubmitAction(@Nullable String type,
                                          @Nullable String value) {
}
