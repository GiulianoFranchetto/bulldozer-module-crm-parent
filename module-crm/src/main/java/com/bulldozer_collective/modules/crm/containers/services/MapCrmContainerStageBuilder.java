package com.bulldozer_collective.modules.crm.containers.services;

import com.bulldozer_collective.modules.crm.api.CrmDealStage;
import com.bulldozer_collective.modules.crm.api.CrmLeadStage;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

/** The new mapping of a stage; both stages {@code null} leave it unmapped. */
@Builder
public record MapCrmContainerStageBuilder(@Nullable CrmDealStage dealStage,
                                          @Nullable CrmLeadStage leadStage,
                                          boolean writeDefault) {
}
