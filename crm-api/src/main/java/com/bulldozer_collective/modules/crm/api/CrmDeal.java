package com.bulldozer_collective.modules.crm.api;

import java.math.BigDecimal;
import java.time.Instant;
import lombok.Builder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A deal as read from the CRM. {@code id} is the CRM's own id.
 *
 * <p>{@code stageValue} is the raw value of the property configured as the stage source of the deal's pipeline;
 * {@code stage} is that value translated through the project's stage mappings. Both are {@code null} when the
 * pipeline has no stage source, and {@code stage} alone is {@code null} when the value is not mapped.
 */
@Builder(toBuilder = true)
public record CrmDeal(@NonNull String id,
                      @Nullable String name,
                      @Nullable BigDecimal amount,
                      @Nullable String currency,
                      @Nullable Instant closeDate,
                      @Nullable String pipelineId,
                      @Nullable CrmDealStage stage,
                      @Nullable String stageValue,
                      @Nullable String ownerId,
                      @Nullable Instant createdAt,
                      @Nullable Instant updatedAt) {
}
