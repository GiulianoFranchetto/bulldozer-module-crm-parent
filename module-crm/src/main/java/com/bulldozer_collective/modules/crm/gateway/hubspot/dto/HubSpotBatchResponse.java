package com.bulldozer_collective.modules.crm.gateway.hubspot.dto;

import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * HubSpot's answer to a batch call: {@code 200} with {@code results}, or {@code 207} (multi-status) with the refused
 * inputs in {@code errors} too.
 */
public record HubSpotBatchResponse<T>(@Nullable List<T> results,
                                      @Nullable List<HubSpotBatchError> errors) {
}
