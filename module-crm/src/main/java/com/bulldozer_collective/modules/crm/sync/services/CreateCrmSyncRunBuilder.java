package com.bulldozer_collective.modules.crm.sync.services;

import com.bulldozer_collective.shared.IntegrationPlatform;
import lombok.Builder;
import org.jspecify.annotations.NonNull;

import java.util.UUID;

@Builder
public record CreateCrmSyncRunBuilder(@NonNull UUID customerId,
                                      @NonNull UUID projectId,
                                      @NonNull IntegrationPlatform platform,
                                      @NonNull UUID jobId) {
}
