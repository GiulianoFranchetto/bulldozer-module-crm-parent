package com.bulldozer_collective.modules.crm.api;

import lombok.Builder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * A property of the CRM's deals or leads — native or custom. Only an enumerated one ({@code options} not empty)
 * can be a container's stage source.
 */
@Builder
public record CrmProperty(@NonNull String name,
                          @NonNull String label,
                          @NonNull String type,
                          @Nullable String fieldType,
                          boolean readOnly,
                          @NonNull List<CrmPropertyOption> options) {
}
