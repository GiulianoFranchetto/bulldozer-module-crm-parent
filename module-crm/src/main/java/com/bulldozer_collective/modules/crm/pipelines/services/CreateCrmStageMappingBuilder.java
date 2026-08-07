package com.bulldozer_collective.modules.crm.pipelines.services;

import com.bulldozer_collective.modules.crm.api.CrmDealStage;
import lombok.Builder;
import org.jspecify.annotations.NonNull;

import java.util.UUID;

@Builder
public record CreateCrmStageMappingBuilder(@NonNull UUID customerId,
                                           @NonNull UUID projectId,
                                           @NonNull UUID pipelineStageId,
                                           @NonNull CrmDealStage internalStage) {
}
