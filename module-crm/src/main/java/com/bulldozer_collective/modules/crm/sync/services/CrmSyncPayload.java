package com.bulldozer_collective.modules.crm.sync.services;

import com.bulldozer_collective.shared.IntegrationPlatform;
import org.jspecify.annotations.NonNull;

import java.util.UUID;

/**
 * Payload carried by every task of a CRM sync job. The OAuth token is deliberately absent: HubSpot access
 * tokens live 30 minutes and the tasks of one job run on separate poller ticks, so each task fetches a fresh
 * token instead of inheriting a stale one.
 */
public record CrmSyncPayload(@NonNull UUID customerId,
                             @NonNull UUID projectId,
                             @NonNull IntegrationPlatform platform,
                             @NonNull UUID syncRunId) {
}
