package com.bulldozer_collective.modules.crm.api;

import lombok.Builder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * A deal as read from the CRM. {@code id} is the CRM's own id; {@code containerId} is the Bulldozer container
 * (the CRM pipeline) the deal was read through.
 *
 * <p>{@code stageValue} is the raw value of the container's stage source property; {@code stage} is the stage
 * that value is mapped to, {@code null} while it is not mapped.
 */
@Builder(toBuilder = true)
public record CrmDeal(@NonNull String id,
                      @Nullable UUID containerId,
                      @Nullable String name,
                      @Nullable BigDecimal amount,
                      @Nullable String currency,
                      @Nullable Instant closeDate,
                      @Nullable CrmDealStage stage,
                      @Nullable String stageValue,
                      @Nullable Instant createdAt,
                      @Nullable Instant updatedAt) {
}
