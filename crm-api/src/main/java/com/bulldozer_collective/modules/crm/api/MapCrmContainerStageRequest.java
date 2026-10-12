package com.bulldozer_collective.modules.crm.api;

import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * Replaces the mapping of stage {@code stageId}. Give {@code dealStage} in a deal container, {@code leadStage} in a
 * lead container; {@code null} leaves the stage unmapped.
 */
public record MapCrmContainerStageRequest(@NonNull @NotNull UUID stageId,
                                          @Nullable CrmDealStage dealStage,
                                          @Nullable CrmLeadStage leadStage,
                                          boolean writeDefault) {
}
