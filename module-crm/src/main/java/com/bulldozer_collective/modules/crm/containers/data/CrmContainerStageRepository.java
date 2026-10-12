package com.bulldozer_collective.modules.crm.containers.data;

import com.bulldozer_collective.shared.jpa.ProjectAwareId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CrmContainerStageRepository extends JpaRepository<CrmContainerStageEntity, ProjectAwareId> {

    List<CrmContainerStageEntity> findAllByCustomerIdAndProjectIdAndContainer_IdOrderByDisplayOrderAsc(
            UUID customerId, UUID projectId, UUID containerId);

    Optional<CrmContainerStageEntity> findByCustomerIdAndProjectIdAndContainer_IdAndId(UUID customerId,
                                                                                       UUID projectId,
                                                                                       UUID containerId,
                                                                                       UUID id);

    @Transactional
    void deleteByCustomerIdAndProjectIdAndContainer_Id(UUID customerId, UUID projectId, UUID containerId);

    @Transactional
    void deleteByCustomerIdAndProjectIdAndContainer_Connection_Id(UUID customerId, UUID projectId, UUID connectionId);
}
