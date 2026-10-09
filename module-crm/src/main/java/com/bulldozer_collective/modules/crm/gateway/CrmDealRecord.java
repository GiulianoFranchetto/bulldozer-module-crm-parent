package com.bulldozer_collective.modules.crm.gateway;

import com.bulldozer_collective.modules.crm.api.CrmDeal;
import org.jspecify.annotations.NonNull;

import java.util.Map;

/**
 * A deal as the gateway reads it: its fields, with {@code stage} and {@code stageValue} still unset, plus the raw
 * properties requested as stage sources. The stage is resolved afterwards, against the project's mappings.
 */
public record CrmDealRecord(@NonNull CrmDeal deal,
                            @NonNull Map<String, String> properties) {
}
