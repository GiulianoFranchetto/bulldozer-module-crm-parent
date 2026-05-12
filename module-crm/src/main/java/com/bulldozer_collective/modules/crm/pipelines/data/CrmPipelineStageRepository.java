package com.bulldozer_collective.modules.crm.pipelines.data;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CrmPipelineStageRepository extends JpaRepository<CrmPipelineStageEntity, UUID> {

    List<CrmPipelineStageEntity> findAllByPipelineId(UUID pipelineId);

    List<CrmPipelineStageEntity> findAllByPipelineIdOrderByDisplayOrderAsc(UUID pipelineId);

    Optional<CrmPipelineStageEntity> findByPipelineIdAndSourceId(UUID pipelineId, String sourceId);
}
