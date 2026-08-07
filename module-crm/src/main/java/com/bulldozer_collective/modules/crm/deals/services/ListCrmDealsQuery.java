package com.bulldozer_collective.modules.crm.deals.services;

import com.bulldozer_collective.modules.crm.api.CrmDealStage;
import com.bulldozer_collective.shared.IntegrationPlatform;
import lombok.Builder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

@Builder
public record ListCrmDealsQuery(@NonNull UUID customerId,
                                @NonNull UUID projectId,
                                @Nullable IntegrationPlatform platform,
                                @Nullable CrmDealStage internalStage,
                                @Nullable UUID pipelineId,
                                int page,
                                int size) {
}
