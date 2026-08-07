package com.bulldozer_collective.modules.crm.sync.services;

import lombok.Builder;
import org.jspecify.annotations.NonNull;

import java.util.UUID;

/**
 * Identifies one sync run. Its only reason to exist is that the three ids are all {@code UUID}: passed
 * positionally they can be swapped without the compiler noticing, and a swapped tenant id on a write is the
 * kind of bug that only shows up in someone else's data.
 */
@Builder
public record CrmSyncRunRef(@NonNull UUID customerId,
                            @NonNull UUID projectId,
                            @NonNull UUID syncRunId) {

    public static CrmSyncRunRef of(@NonNull CrmSyncPayload payload) {
        return new CrmSyncRunRef(payload.customerId(), payload.projectId(), payload.syncRunId());
    }
}
