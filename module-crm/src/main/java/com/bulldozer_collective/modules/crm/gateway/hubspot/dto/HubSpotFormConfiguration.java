package com.bulldozer_collective.modules.crm.gateway.hubspot.dto;

import org.jspecify.annotations.Nullable;

public record HubSpotFormConfiguration(@Nullable String language,
                                       @Nullable HubSpotFormPostSubmitAction postSubmitAction) {
}
