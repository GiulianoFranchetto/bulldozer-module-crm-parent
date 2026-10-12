package com.bulldozer_collective.modules.crm.activities.services;

import com.bulldozer_collective.modules.crm.api.CrmActivityType;
import com.bulldozer_collective.modules.crm.api.CrmObjectRef;
import lombok.Builder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.List;

/**
 * An activity to log in the CRM, then attach to {@code associations}.
 *
 * @param timestamp when the activity happened — for a task, its due date.
 * @param status    the CRM's raw status value.
 */
@Builder
public record CreateCrmActivityBuilder(@NonNull CrmActivityType type,
                                       @NonNull Instant timestamp,
                                       @Nullable String title,
                                       @Nullable String body,
                                       @Nullable String status,
                                       @NonNull List<CrmObjectRef> associations) {
}
