package com.bulldozer_collective.modules.crm.pipelines.services;

import com.bulldozer_collective.shared.IntegrationPlatform;
import com.bulldozer_collective.modules.crm.pipelines.data.CrmPipelineEntity;
import com.bulldozer_collective.modules.crm.pipelines.data.CrmPipelineRepository;
import com.bulldozer_collective.modules.crm.pipelines.data.CrmPipelineStageEntity;
import com.bulldozer_collective.modules.crm.pipelines.data.CrmPipelineStageRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CrmPipelineUpsertService {

    private final CrmPipelineRepository pipelineRepository;
    private final CrmPipelineStageRepository stageRepository;

    @Transactional
    public CrmPipelineEntity upsertPipeline(@NonNull UUID customerId,
                                            @NonNull UUID projectId,
                                            @NonNull IntegrationPlatform platform,
                                            @NonNull String sourceId,
                                            @NonNull String name) {
        return this.pipelineRepository
                .findByCustomerIdAndProjectIdAndPlatformAndSourceId(customerId, projectId, platform, sourceId)
                .map(existing -> {
                    existing.setName(name);
                    if (existing.getDeletedAt() != null) {
                        this.pipelineRepository.reactivateBySourceId(customerId, projectId, platform, sourceId);
                    }
                    return this.pipelineRepository.save(existing);
                })
                .orElseGet(() -> {
                    var entity = new CrmPipelineEntity();
                    entity.setCustomerId(customerId);
                    entity.setProjectId(projectId);
                    entity.setPlatform(platform);
                    entity.setSourceId(sourceId);
                    entity.setName(name);
                    return this.pipelineRepository.save(entity);
                });
    }

    @Transactional
    public CrmPipelineStageEntity upsertStage(@NonNull UUID pipelineId,
                                              @NonNull String sourceId,
                                              @NonNull String label,
                                              int displayOrder) {
        return this.stageRepository
                .findByPipelineIdAndSourceId(pipelineId, sourceId)
                .map(existing -> {
                    existing.setLabel(label);
                    existing.setDisplayOrder(displayOrder);
                    return this.stageRepository.save(existing);
                })
                .orElseGet(() -> {
                    var entity = new CrmPipelineStageEntity();
                    entity.setPipelineId(pipelineId);
                    entity.setSourceId(sourceId);
                    entity.setLabel(label);
                    entity.setDisplayOrder(displayOrder);
                    return this.stageRepository.save(entity);
                });
    }
}
