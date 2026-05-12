package com.bulldozer_collective.modules.crm.pipelines.data;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CrmDealStageMappingRepository extends JpaRepository<CrmDealStageMappingEntity, UUID> {

    List<CrmDealStageMappingEntity> findAllByCustomerIdAndProjectId(UUID customerId, UUID projectId);

    Optional<CrmDealStageMappingEntity> findByPipelineStageId(UUID pipelineStageId);

    void deleteByPipelineStageIdAndCustomerIdAndProjectId(UUID pipelineStageId, UUID customerId, UUID projectId);
}
