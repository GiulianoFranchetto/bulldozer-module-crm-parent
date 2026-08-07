package com.bulldozer_collective.modules.crm.deals.services;

import com.bulldozer_collective.modules.crm.api.CrmDealStage;
import lombok.Builder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * One deal as the CRM reports it. The pipeline, stage and internal stage are already resolved to Bulldozer ids
 * by the adapter: a deal whose stage carries no mapping is never turned into an upsert at all.
 */
@Builder
public record CrmDealUpsert(@NonNull String sourceId,
                            @NonNull String name,
                            @Nullable BigDecimal amount,
                            @Nullable String currency,
                            @Nullable Instant closeDate,
                            @NonNull CrmDealStage internalStage,
                            @NonNull UUID pipelineId,
                            @NonNull UUID pipelineStageId) {
}
