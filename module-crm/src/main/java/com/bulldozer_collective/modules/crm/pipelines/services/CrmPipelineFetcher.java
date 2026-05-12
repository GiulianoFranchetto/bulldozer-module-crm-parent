package com.bulldozer_collective.modules.crm.pipelines.services;

import com.bulldozer_collective.shared.IntegrationPlatform;
import com.bulldozer_collective.modules.crm.pipelines.data.CrmDealStageMappingRepository;
import com.bulldozer_collective.modules.crm.pipelines.data.CrmPipelineEntity;
import com.bulldozer_collective.modules.crm.pipelines.data.CrmPipelineRepository;
import com.bulldozer_collective.modules.crm.pipelines.data.CrmPipelineStageEntity;
import com.bulldozer_collective.modules.crm.pipelines.data.CrmPipelineStageRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CrmPipelineFetcher {

    private final CrmPipelineRepository pipelineRepository;
    private final CrmPipelineStageRepository stageRepository;
    private final CrmDealStageMappingRepository mappingRepository;

    public List<CrmPipelineEntity> fetchAll(@NonNull UUID customerId,
                                            @NonNull UUID projectId,
                                            @Nullable IntegrationPlatform platform) {
        if (platform != null) {
            return this.pipelineRepository.findAllByCustomerIdAndProjectIdAndPlatformAndDeletedAtIsNull(customerId, projectId, platform);
        }
        return this.pipelineRepository.findAllByCustomerIdAndProjectIdAndDeletedAtIsNull(customerId, projectId);
    }

    public List<CrmPipelineStageEntity> fetchStages(@NonNull UUID pipelineId) {
        return this.stageRepository.findAllByPipelineIdOrderByDisplayOrderAsc(pipelineId);
    }
}
