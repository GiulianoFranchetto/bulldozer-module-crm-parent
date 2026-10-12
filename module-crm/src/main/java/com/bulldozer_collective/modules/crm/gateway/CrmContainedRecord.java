package com.bulldozer_collective.modules.crm.gateway;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * A deal or a lead as the gateway reads it: the record with its {@code containerId} and stage still unset, the
 * CRM id of the pipeline it belongs to, and the raw properties read with it — the stage source among them. The
 * services check the pipeline against the container and resolve the stage afterwards.
 */
public record CrmContainedRecord<T>(@NonNull T item,
                                    @Nullable String containerRemoteId,
                                    @NonNull Map<String, String> properties) {
}
