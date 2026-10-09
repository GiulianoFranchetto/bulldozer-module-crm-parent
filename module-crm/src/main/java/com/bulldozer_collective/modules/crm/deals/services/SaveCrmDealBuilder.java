package com.bulldozer_collective.modules.crm.deals.services;

import com.bulldozer_collective.modules.crm.api.CrmDealStage;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.stream.Stream;

/**
 * The fields of a deal to write to the CRM. A {@code null} field is not written. {@code stage} is not written as
 * such: the services translate it into the CRM value its mapping designates.
 */
@Builder
public record SaveCrmDealBuilder(@Nullable String name,
                                 @Nullable String pipelineId,
                                 @Nullable CrmDealStage stage,
                                 @Nullable BigDecimal amount,
                                 @Nullable String currency,
                                 @Nullable Instant closeDate,
                                 @Nullable String ownerId) {

    public boolean isEmpty() {
        return Stream.of(this.name, this.pipelineId, this.stage, this.amount, this.currency, this.closeDate,
                        this.ownerId)
                .allMatch(value -> value == null);
    }
}
