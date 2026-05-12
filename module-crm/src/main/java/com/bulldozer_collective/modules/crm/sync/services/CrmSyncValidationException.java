package com.bulldozer_collective.modules.crm.sync.services;

import com.bulldozer_collective.shared.IntegrationPlatform;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.UUID;

public class CrmSyncValidationException extends RuntimeException {

    public CrmSyncValidationException(@NonNull UUID customerId,
                                      @NonNull UUID projectId,
                                      @NonNull IntegrationPlatform platform,
                                      @NonNull List<String> unmappedStageLabels) {
        super("Cannot sync %s for project %s: unmapped pipeline stages: %s"
                .formatted(platform.getNiceName(), projectId, String.join(", ", unmappedStageLabels)));
    }
}
