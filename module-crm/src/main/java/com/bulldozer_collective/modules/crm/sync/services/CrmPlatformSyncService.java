package com.bulldozer_collective.modules.crm.sync.services;

import com.bulldozer_collective.shared.OAuthToken;
import org.jspecify.annotations.NonNull;

import java.util.UUID;

/**
 * A CRM platform adapter. The three methods are the three steps of a sync, run as separate job tasks in this
 * order: pipelines must exist (and be mapped) before deals can be resolved, and the objects must exist before
 * their associations can be linked.
 *
 * <p>No implementation may be {@code @Transactional}: every method performs remote HTTP calls, and holding a
 * pooled connection across them exhausts the pool. Persistence happens per page / per batch instead.
 */
public interface CrmPlatformSyncService {

    void discoverPipelines(@NonNull UUID customerId,
                           @NonNull UUID projectId,
                           @NonNull OAuthToken token);

    CrmSyncResult syncObjects(@NonNull UUID customerId,
                              @NonNull UUID projectId,
                              @NonNull OAuthToken token);

    void syncAssociations(@NonNull UUID customerId,
                          @NonNull UUID projectId,
                          @NonNull OAuthToken token);
}
