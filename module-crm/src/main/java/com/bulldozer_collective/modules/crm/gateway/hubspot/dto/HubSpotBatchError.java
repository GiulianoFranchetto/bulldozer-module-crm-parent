package com.bulldozer_collective.modules.crm.gateway.hubspot.dto;

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * One refused input of a batch call. When the request gave each input an {@code objectWriteTraceId}, {@code context}
 * lists the refused ones under that key.
 */
public record HubSpotBatchError(@Nullable String message,
                                @Nullable Map<String, List<String>> context) {
}
