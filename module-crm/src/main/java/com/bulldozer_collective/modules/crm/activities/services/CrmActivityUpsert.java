package com.bulldozer_collective.modules.crm.activities.services;

import com.bulldozer_collective.modules.crm.api.CrmActivityType;
import lombok.Builder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

/**
 * One activity (call, email, meeting, note, task) as the CRM reports it.
 */
@Builder
public record CrmActivityUpsert(@NonNull String sourceId,
                                @NonNull CrmActivityType type,
                                @Nullable String title,
                                @Nullable String body,
                                @Nullable Instant dueDate,
                                @Nullable Instant completedAt,
                                @Nullable UUID dealId,
                                @Nullable UUID contactId,
                                @Nullable UUID companyId) {
}
