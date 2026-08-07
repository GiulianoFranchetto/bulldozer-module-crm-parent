package com.bulldozer_collective.modules.crm.associations.services;

import lombok.Builder;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * One atomic association replacement: the owners whose links are being rewritten, and their new targets.
 *
 * @param ownerIds  the owners in this batch, capped at {@link CrmAssociationPersister#MAX_OWNERS_PER_BATCH}
 * @param targetsByOwner new links, keyed by owner. An owner absent from the map ends up with no link at all —
 *                       which is the point: the CRM is the source of truth and a sync replaces rather than merges.
 */
@Builder
public record ReplaceCrmLinksBuilder(@NonNull UUID customerId,
                                     @NonNull UUID projectId,
                                     @NonNull List<UUID> ownerIds,
                                     @NonNull Map<UUID, List<UUID>> targetsByOwner) {
}
