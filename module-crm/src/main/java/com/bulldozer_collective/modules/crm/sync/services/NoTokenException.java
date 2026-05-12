package com.bulldozer_collective.modules.crm.sync.services;

import com.bulldozer_collective.shared.IntegrationPlatform;
import org.jspecify.annotations.NonNull;

import java.util.UUID;

public class NoTokenException extends Exception {

    public NoTokenException(@NonNull UUID customerId,
                            @NonNull UUID projectId,
                            @NonNull IntegrationPlatform platform) {
        super("No %s token found for customer %s, project %s"
                .formatted(platform.getNiceName(), customerId, projectId));
    }
}
