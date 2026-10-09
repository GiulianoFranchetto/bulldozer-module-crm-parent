package com.bulldozer_collective.modules.crm.stages.services;

import com.bulldozer_collective.modules.crm.api.CrmDealStage;
import lombok.Builder;
import org.jspecify.annotations.NonNull;

@Builder
public record CreateCrmStageMappingBuilder(@NonNull String value,
                                           @NonNull CrmDealStage stage,
                                           boolean writeDefault) {
}
