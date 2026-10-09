package com.bulldozer_collective.modules.crm.api;

import java.util.List;
import lombok.Builder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A deal property of the CRM — native or custom — that can serve as a pipeline's stage source. {@code options}
 * lists the accepted values of an enumerated property, and is empty for a free-form one.
 */
@Builder
public record CrmDealProperty(@NonNull String name,
                              @NonNull String label,
                              @NonNull String type,
                              @Nullable String fieldType,
                              boolean readOnly,
                              @NonNull List<CrmDealPropertyOption> options) {
}
