package com.bulldozer_collective.modules.crm.api;

import lombok.Builder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.List;

/**
 * A form of the CRM's marketing tool. After a submission, the form shows {@code thankYouMessage} or sends to
 * {@code redirectUrl} — at most one is set.
 */
@Builder
public record CrmForm(@NonNull String id,
                      @NonNull String name,
                      @NonNull List<CrmFormField> fields,
                      @Nullable String submitButtonText,
                      @Nullable String thankYouMessage,
                      @Nullable String redirectUrl,
                      @Nullable String language,
                      @NonNull CrmFormLegalConsent legalConsent,
                      @Nullable Instant createdAt,
                      @Nullable Instant updatedAt) {
}
