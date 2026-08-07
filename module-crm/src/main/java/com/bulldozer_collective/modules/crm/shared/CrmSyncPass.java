package com.bulldozer_collective.modules.crm.shared;

import com.bulldozer_collective.shared.IntegrationPlatform;
import lombok.Builder;
import org.jspecify.annotations.NonNull;

import java.time.Instant;
import java.util.UUID;

/**
 * Identifies one sweep of one CRM for one project: the tenant, the platform, and the instant the sweep started.
 *
 * <p>{@code startedAt} is the pivot of the soft-delete strategy — every row a persister writes is stamped with
 * it, and closing the pass soft-deletes whatever still carries an older stamp. It must therefore be the same
 * value for every page of every object type in a sweep, which is exactly what passing it around as one object
 * makes hard to get wrong.
 *
 * <p>Lives in {@code shared} rather than in {@code sync}: the persisters live in their own domain packages, and
 * having them import from {@code sync} — which already imports them — would close a package cycle.
 */
@Builder
public record CrmSyncPass(@NonNull UUID customerId,
                          @NonNull UUID projectId,
                          @NonNull IntegrationPlatform platform,
                          @NonNull Instant startedAt) {
}
