package com.bulldozer_collective.modules.crm.containers.services;

import com.bulldozer_collective.modules.crm.api.CrmDealStage;
import com.bulldozer_collective.modules.crm.api.CrmLeadStage;
import lombok.Builder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/** The new mapping of one stage; both stages {@code null} leave it unmapped. */
@Builder
public record MapCrmContainerStageBuilder(@NonNull UUID stageId,
                                          @Nullable CrmDealStage dealStage,
                                          @Nullable CrmLeadStage leadStage,
                                          boolean writeDefault) {

    public boolean mapped() {
        return this.dealStage != null || this.leadStage != null;
    }
}
