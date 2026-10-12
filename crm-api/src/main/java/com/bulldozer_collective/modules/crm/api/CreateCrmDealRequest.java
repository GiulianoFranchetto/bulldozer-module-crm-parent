package com.bulldozer_collective.modules.crm.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * The deal is created in the container's pipeline; {@code stage} is written as the value mapped to it.
 * {@code pipelineStage} is the id of the pipeline's own stage (from {@code GET …/containers}): required when the
 * container reads its stages from a custom property, since the CRM files every deal under one of its pipeline's
 * stages; refused otherwise, {@code stage} already designating it.
 */
public record CreateCrmDealRequest(@NonNull @NotBlank @Size(max = CrmConstraints.SHORT_TEXT) String name,
                                   @NonNull @NotNull CrmDealStage stage,
                                   @Nullable @PositiveOrZero BigDecimal amount,
                                   @Nullable @Size(min = 3, max = 3) String currency,
                                   @Nullable Instant closeDate,
                                   @Nullable @Size(max = CrmConstraints.SHORT_TEXT) String pipelineStage) {
}
