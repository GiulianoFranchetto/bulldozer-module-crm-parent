package com.bulldozer_collective.modules.crm.stages.data;

import com.bulldozer_collective.modules.crm.api.CrmDealStage;
import com.bulldozer_collective.shared.jpa.ProjectAwareId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CrmStageMappingRepository extends JpaRepository<CrmStageMappingEntity, ProjectAwareId> {

    List<CrmStageMappingEntity> findAllByCustomerIdAndProjectIdAndConnectionId(UUID customerId,
                                                                               UUID projectId,
                                                                               UUID connectionId);

    List<CrmStageMappingEntity> findAllByCustomerIdAndProjectIdAndConnectionIdAndPipelineIdOrderByCreatedAtAsc(
            UUID customerId, UUID projectId, UUID connectionId, String pipelineId);

    List<CrmStageMappingEntity> findAllByCustomerIdAndProjectIdAndConnectionIdAndPipelineIdAndStage(
            UUID customerId, UUID projectId, UUID connectionId, String pipelineId, CrmDealStage stage);

    Optional<CrmStageMappingEntity> findByCustomerIdAndProjectIdAndConnectionIdAndPipelineIdAndId(
            UUID customerId, UUID projectId, UUID connectionId, String pipelineId, UUID id);

    boolean existsByCustomerIdAndProjectIdAndConnectionIdAndPipelineIdAndValue(
            UUID customerId, UUID projectId, UUID connectionId, String pipelineId, String value);

    @Transactional
    void deleteByCustomerIdAndProjectIdAndConnectionId(UUID customerId, UUID projectId, UUID connectionId);

    @Transactional
    void deleteByCustomerIdAndProjectIdAndConnectionIdAndPipelineId(UUID customerId,
                                                                    UUID projectId,
                                                                    UUID connectionId,
                                                                    String pipelineId);
}
