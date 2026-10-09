package com.bulldozer_collective.modules.crm.api;

import lombok.Builder;
import org.jspecify.annotations.NonNull;

@Builder
public record CrmDealPropertyOption(@NonNull String value,
                                    @NonNull String label,
                                    int displayOrder,
                                    boolean hidden) {
}
