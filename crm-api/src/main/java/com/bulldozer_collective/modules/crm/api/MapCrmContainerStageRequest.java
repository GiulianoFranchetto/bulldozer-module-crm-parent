package com.bulldozer_collective.modules.crm.api;

import org.jspecify.annotations.Nullable;

/**
 * Replaces the stage's mapping. Give {@code dealStage} in a deal container, {@code leadStage} in a lead
 * container; {@code null} leaves the stage unmapped.
 */
public record MapCrmContainerStageRequest(@Nullable CrmDealStage dealStage,
                                          @Nullable CrmLeadStage leadStage,
                                          boolean writeDefault) {
}
