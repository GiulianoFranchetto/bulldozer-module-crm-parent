package com.bulldozer_collective.modules.crm.api;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import org.jspecify.annotations.Nullable;

/**
 * A {@code null} field is left unchanged in the CRM. Moving a deal to another pipeline requires its stage in
 * that pipeline: {@code pipelineId} without {@code stage} is rejected.
 */
public record UpdateCrmDealRequest(@Nullable @Size(max = CrmConstraints.SHORT_TEXT) String name,
                                   @Nullable @Size(max = CrmConstraints.SHORT_TEXT) String pipelineId,
                                   @Nullable CrmDealStage stage,
                                   @Nullable @PositiveOrZero BigDecimal amount,
                                   @Nullable @Size(min = 3, max = 3) String currency,
                                   @Nullable Instant closeDate,
                                   @Nullable @Size(max = CrmConstraints.SHORT_TEXT) String ownerId) {
}
