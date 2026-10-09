package com.bulldozer_collective.modules.crm.stages.data;

import com.bulldozer_collective.shared.jpa.ProjectAwareId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CrmStageSourceRepository extends JpaRepository<CrmStageSourceEntity, ProjectAwareId> {

    List<CrmStageSourceEntity> findAllByCustomerIdAndProjectIdAndConnectionId(UUID customerId,
                                                                              UUID projectId,
                                                                              UUID connectionId);

    Optional<CrmStageSourceEntity> findByCustomerIdAndProjectIdAndConnectionIdAndPipelineId(UUID customerId,
                                                                                            UUID projectId,
                                                                                            UUID connectionId,
                                                                                            String pipelineId);

    @Transactional
    void deleteByCustomerIdAndProjectIdAndConnectionId(UUID customerId, UUID projectId, UUID connectionId);
}
