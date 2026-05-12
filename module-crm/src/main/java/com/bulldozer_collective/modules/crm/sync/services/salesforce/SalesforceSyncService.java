package com.bulldozer_collective.modules.crm.sync.services.salesforce;

import com.bulldozer_collective.modules.crm.sync.services.CrmPlatformSyncService;
import com.bulldozer_collective.modules.crm.sync.services.CrmSyncResult;
import com.bulldozer_collective.shared.OAuthToken;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.util.UUID;

// TODO: implement using Salesforce REST API
@Service
@RequiredArgsConstructor
public class SalesforceSyncService implements CrmPlatformSyncService {

    @Override
    public void discoverPipelines(@NonNull UUID customerId,
                                  @NonNull UUID projectId,
                                  @NonNull OAuthToken token) throws Exception {
        throw new UnsupportedOperationException("Salesforce pipeline discovery not yet implemented");
    }

    @Override
    public CrmSyncResult sync(@NonNull UUID customerId,
                              @NonNull UUID projectId,
                              @NonNull OAuthToken token) throws Exception {
        throw new UnsupportedOperationException("Salesforce sync not yet implemented");
    }
}
