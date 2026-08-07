package com.bulldozer_collective.modules.crm.pipelines.services;

import com.bulldozer_collective.shared.IntegrationPlatform;
import com.bulldozer_collective.modules.crm.pipelines.data.CrmPipelineEntity;
import com.bulldozer_collective.modules.crm.pipelines.data.CrmPipelineRepository;
import com.bulldozer_collective.modules.crm.sync.services.CrmSyncPlatformResolver;
import com.bulldozer_collective.modules.crm.sync.services.NoTokenException;
import com.bulldozer_collective.modules.crm.sync.services.PlatformTokenFetcher;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CrmPipelineDiscoveryService {

    private final PlatformTokenFetcher tokenFetcher;
    private final CrmSyncPlatformResolver platformResolver;
    private final CrmPipelineRepository pipelineRepository;

    public List<CrmPipelineEntity> discoverAndFetch(@NonNull UUID customerId,
                                                    @NonNull UUID projectId,
                                                    @NonNull IntegrationPlatform platform) throws NoTokenException {
        var service = this.platformResolver.resolve(platform);
        var token = this.tokenFetcher.fetchToken(customerId, projectId, platform);
        service.discoverPipelines(customerId, projectId, token);
        return this.pipelineRepository.findAllByCustomerIdAndProjectIdAndPlatformAndDeletedAtIsNull(
                customerId, projectId, platform);
    }
}
