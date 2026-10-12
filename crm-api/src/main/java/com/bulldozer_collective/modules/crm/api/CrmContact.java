package com.bulldozer_collective.modules.crm.api;

import lombok.Builder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

/** A contact as read from the CRM. {@code id} is the CRM's own id. */
@Builder
public record CrmContact(@NonNull String id,
                         @Nullable String firstName,
                         @Nullable String lastName,
                         @Nullable String email,
                         @Nullable String phone,
                         @Nullable String jobTitle,
                         @Nullable Instant createdAt,
                         @Nullable Instant updatedAt) {
}
