package com.bulldozer_collective.modules.crm.sync.services;

import com.bulldozer_collective.modules.crm.api.CrmSyncStatus;
import com.bulldozer_collective.shared.IntegrationPlatform;
import lombok.Builder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

@Builder
public record ListCrmSyncRunsQuery(@NonNull UUID customerId,
                                   @NonNull UUID projectId,
                                   @Nullable IntegrationPlatform platform,
                                   @Nullable CrmSyncStatus status,
                                   int page,
                                   int size) {
}
