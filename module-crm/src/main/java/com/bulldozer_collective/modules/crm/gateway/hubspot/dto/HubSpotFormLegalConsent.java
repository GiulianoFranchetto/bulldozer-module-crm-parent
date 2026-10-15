package com.bulldozer_collective.modules.crm.gateway.hubspot.dto;

import org.jspecify.annotations.Nullable;

/** @param type {@code none}, {@code legitimate_interest}, {@code explicit_consent_to_process}… */
public record HubSpotFormLegalConsent(@Nullable String type,
                                      @Nullable String lawfulBasis,
                                      @Nullable String privacyText) {
}
