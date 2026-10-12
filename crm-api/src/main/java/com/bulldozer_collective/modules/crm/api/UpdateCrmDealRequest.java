package com.bulldozer_collective.modules.crm.api;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.Instant;

/** A {@code null} field is left unchanged in the CRM. A deal never leaves its container. */
public record UpdateCrmDealRequest(@Nullable @Size(max = CrmConstraints.SHORT_TEXT) String name,
                                   @Nullable CrmDealStage stage,
                                   @Nullable @PositiveOrZero BigDecimal amount,
                                   @Nullable @Size(min = 3, max = 3) String currency,
                                   @Nullable Instant closeDate) {
}
