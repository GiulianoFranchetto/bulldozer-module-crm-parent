package com.bulldozer_collective.modules.crm.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** {@code stage} is written as the value its mapping designates in {@code pipelineId}. */
public record CreateCrmDealRequest(@NonNull @NotBlank @Size(max = CrmConstraints.SHORT_TEXT) String name,
                                   @NonNull @NotBlank @Size(max = CrmConstraints.SHORT_TEXT) String pipelineId,
                                   @NonNull @NotNull CrmDealStage stage,
                                   @Nullable @PositiveOrZero BigDecimal amount,
                                   @Nullable @Size(min = 3, max = 3) String currency,
                                   @Nullable Instant closeDate,
                                   @Nullable @Size(max = CrmConstraints.SHORT_TEXT) String ownerId) {
}
