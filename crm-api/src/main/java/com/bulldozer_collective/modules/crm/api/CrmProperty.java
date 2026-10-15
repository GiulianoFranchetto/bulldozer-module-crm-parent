package com.bulldozer_collective.modules.crm.api;

import lombok.Builder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * A property of a CRM record type — native or custom. Only an enumerated one ({@code options} not empty) of a deal
 * or a lead can be a container's stage source.
 *
 * @param groupName the CRM's property group the property is filed under, when the CRM has groups.
 */
@Builder
public record CrmProperty(@NonNull String name,
                          @NonNull String label,
                          @NonNull String type,
                          @Nullable String fieldType,
                          @Nullable String groupName,
                          boolean readOnly,
                          @NonNull List<CrmPropertyOption> options) {
}
