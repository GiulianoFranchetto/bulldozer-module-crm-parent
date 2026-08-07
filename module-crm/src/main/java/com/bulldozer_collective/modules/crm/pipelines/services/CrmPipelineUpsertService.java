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

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CrmPipelineUpsertService {

    private final CrmPipelineRepository pipelineRepository;
    private final CrmPipelineStageRepository stageRepository;

    @Transactional
    public CrmPipelineEntity upsertPipeline(@NonNull UpsertCrmPipelineBuilder request) {
        var customerId = request.customerId();
        var projectId = request.projectId();
        var platform = request.platform();
        var sourceId = request.sourceId();
        var name = request.name();

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
    public CrmPipelineStageEntity upsertStage(@NonNull UpsertCrmPipelineStageBuilder request) {
        var customerId = request.customerId();
        var projectId = request.projectId();
        var pipelineId = request.pipelineId();
        var sourceId = request.sourceId();
        var label = request.label();
        var displayOrder = request.displayOrder();

        return this.stageRepository
                .findByPipelineIdAndSourceId(pipelineId, sourceId)
                .map(existing -> {
                    existing.setLabel(label);
                    existing.setDisplayOrder(displayOrder);
                    // A stage that reappears in the CRM must come back, mappings and all. It takes a bulk
                    // update: deletedAt is @Column(updatable = false), so setting it on the entity is a no-op.
                    if (existing.getDeletedAt() != null) {
                        this.stageRepository.reactivateBySourceId(pipelineId, sourceId);
                    }
                    return this.stageRepository.save(existing);
                })
                .orElseGet(() -> {
                    var entity = new CrmPipelineStageEntity();
                    entity.setCustomerId(customerId);
                    entity.setProjectId(projectId);
                    entity.setPipelineId(pipelineId);
                    entity.setSourceId(sourceId);
                    entity.setLabel(label);
                    entity.setDisplayOrder(displayOrder);
                    return this.stageRepository.save(entity);
                });
    }

    /**
     * Soft-deletes the stages of a pipeline the platform no longer returns, so a stage removed in the CRM
     * stops being demanded by {@code CrmSyncValidator}.
     */
    @Transactional
    public void pruneStages(@NonNull UUID pipelineId, @NonNull Set<String> seenSourceIds) {
        if (seenSourceIds.isEmpty()) {
            return;
        }
        this.stageRepository.softDeleteMissing(pipelineId, seenSourceIds, Instant.now());
    }
}
