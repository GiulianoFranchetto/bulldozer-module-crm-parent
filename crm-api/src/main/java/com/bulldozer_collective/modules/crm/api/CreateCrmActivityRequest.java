package com.bulldozer_collective.modules.crm.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * {@code timestamp} is when the activity happened — for a task, its due date. {@code status} is the CRM's raw
 * status value (e.g. HubSpot's {@code COMPLETED} for a call or a task). {@code associations} lists the records
 * the activity is attached to once created.
 */
public record CreateCrmActivityRequest(@NonNull @NotNull CrmActivityType type,
                                       @NonNull @NotNull Instant timestamp,
                                       @Nullable @Size(max = CrmConstraints.SHORT_TEXT) String title,
                                       @Nullable @Size(max = CrmConstraints.LONG_TEXT) String body,
                                       @Nullable @Size(max = 32) String status,
                                       @Nullable @Size(max = CrmConstraints.SHORT_TEXT) String ownerId,
                                       @Nullable @Size(max = 50) List<@Valid CrmObjectRef> associations) {
}
