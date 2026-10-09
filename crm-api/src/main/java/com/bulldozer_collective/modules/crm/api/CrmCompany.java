package com.bulldozer_collective.modules.crm.api;

import java.time.Instant;
import lombok.Builder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** A company as read from the CRM. {@code id} is the CRM's own id. */
@Builder
public record CrmCompany(@NonNull String id,
                         @Nullable String name,
                         @Nullable String domain,
                         @Nullable String industry,
                         @Nullable String phone,
                         @Nullable String city,
                         @Nullable String country,
                         @Nullable String ownerId,
                         @Nullable Instant createdAt,
                         @Nullable Instant updatedAt) {
}
