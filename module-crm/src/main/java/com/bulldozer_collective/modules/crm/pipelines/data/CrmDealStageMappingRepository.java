package com.bulldozer_collective.modules.crm.pipelines.data;

import com.bulldozer_collective.shared.jpa.ProjectAwareId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CrmDealStageMappingRepository extends JpaRepository<CrmDealStageMappingEntity, ProjectAwareId> {

    List<CrmDealStageMappingEntity> findAllByCustomerIdAndProjectId(UUID customerId, UUID projectId);

    Optional<CrmDealStageMappingEntity> findByPipelineStageIdAndCustomerIdAndProjectId(UUID pipelineStageId, UUID customerId, UUID projectId);

    Optional<CrmDealStageMappingEntity> findByIdAndCustomerIdAndProjectId(UUID id, UUID customerId, UUID projectId);

    @Transactional
    void deleteByPipelineStageIdAndCustomerIdAndProjectId(UUID pipelineStageId, UUID customerId, UUID projectId);
}
