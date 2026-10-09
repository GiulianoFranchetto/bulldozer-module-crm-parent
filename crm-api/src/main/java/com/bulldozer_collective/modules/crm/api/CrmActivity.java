package com.bulldozer_collective.modules.crm.api;

import java.time.Instant;
import lombok.Builder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * An activity (call, email, meeting, note or task) as read from the CRM. {@code timestamp} is when it happened —
 * for a task, its due date. {@code status} is the CRM's raw status (call, email or task status).
 */
@Builder
public record CrmActivity(@NonNull String id,
                          @NonNull CrmActivityType type,
                          @Nullable String title,
                          @Nullable String body,
                          @Nullable String status,
                          @Nullable Instant timestamp,
                          @Nullable String ownerId,
                          @Nullable Instant createdAt,
                          @Nullable Instant updatedAt) {
}
