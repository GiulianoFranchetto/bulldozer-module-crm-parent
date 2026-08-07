package com.bulldozer_collective.modules.crm.activities.services;

import com.bulldozer_collective.modules.crm.api.CrmActivityType;
import com.bulldozer_collective.shared.IntegrationPlatform;
import lombok.Builder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

@Builder
public record ListCrmActivitiesQuery(@NonNull UUID customerId,
                                     @NonNull UUID projectId,
                                     @Nullable IntegrationPlatform platform,
                                     @Nullable CrmActivityType type,
                                     @Nullable UUID dealId,
                                     @Nullable UUID contactId,
                                     @Nullable UUID companyId,
                                     int page,
                                     int size) {
}
