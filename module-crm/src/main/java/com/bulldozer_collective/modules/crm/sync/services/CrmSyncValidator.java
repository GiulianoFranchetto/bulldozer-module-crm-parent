package com.bulldozer_collective.modules.crm.sync.services;

import com.bulldozer_collective.shared.IntegrationPlatform;
import com.bulldozer_collective.modules.crm.pipelines.data.CrmDealStageMappingRepository;
import com.bulldozer_collective.modules.crm.pipelines.data.CrmPipelineRepository;
import com.bulldozer_collective.modules.crm.pipelines.data.CrmPipelineStageRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CrmSyncValidator {

    private final CrmPipelineRepository pipelineRepository;
    private final CrmPipelineStageRepository stageRepository;
    private final CrmDealStageMappingRepository mappingRepository;

    public void validate(@NonNull UUID customerId,
                         @NonNull UUID projectId,
                         @NonNull IntegrationPlatform platform) {
        var pipelines = this.pipelineRepository
                .findAllByCustomerIdAndProjectIdAndPlatformAndDeletedAtIsNull(customerId, projectId, platform);

        List<String> unmapped = new ArrayList<>();
        for (var pipeline : pipelines) {
            var stages = this.stageRepository.findAllByPipelineIdAndDeletedAtIsNull(pipeline.getId());
            for (var stage : stages) {
                if (this.mappingRepository.findByPipelineStageIdAndCustomerIdAndProjectId(
                        stage.getId(), customerId, projectId).isEmpty()) {
                    unmapped.add("[%s] %s".formatted(pipeline.getName(), stage.getLabel()));
                }
            }
        }

        if (!unmapped.isEmpty()) {
            throw new CrmSyncValidationException(customerId, projectId, platform, unmapped);
        }
    }
}
