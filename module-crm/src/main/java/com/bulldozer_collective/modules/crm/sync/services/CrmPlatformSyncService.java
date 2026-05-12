package com.bulldozer_collective.modules.crm.sync.services;

import com.bulldozer_collective.shared.OAuthToken;
import org.jspecify.annotations.NonNull;

import java.util.UUID;

public interface CrmPlatformSyncService {

    void discoverPipelines(@NonNull UUID customerId,
                           @NonNull UUID projectId,
                           @NonNull OAuthToken token) throws Exception;

    CrmSyncResult sync(@NonNull UUID customerId,
                       @NonNull UUID projectId,
                       @NonNull OAuthToken token) throws Exception;
}
