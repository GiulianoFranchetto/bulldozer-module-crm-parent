package com.bulldozer_collective.modules.crm.containers.data;

import com.bulldozer_collective.modules.crm.api.CrmContainerType;
import com.bulldozer_collective.shared.jpa.ProjectAwareId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CrmContainerRepository extends JpaRepository<CrmContainerEntity, ProjectAwareId> {

    List<CrmContainerEntity> findAllByCustomerIdAndProjectIdAndConnection_IdOrderByCreatedAtAsc(UUID customerId,
                                                                                               UUID projectId,
                                                                                               UUID connectionId);

    List<CrmContainerEntity> findAllByCustomerIdAndProjectIdAndConnection_IdAndTypeOrderByCreatedAtAsc(
            UUID customerId, UUID projectId, UUID connectionId, CrmContainerType type);

    Optional<CrmContainerEntity> findByCustomerIdAndProjectIdAndConnection_IdAndId(UUID customerId,
                                                                                   UUID projectId,
                                                                                   UUID connectionId,
                                                                                   UUID id);

    boolean existsByCustomerIdAndProjectIdAndConnection_IdAndTypeAndRemoteId(UUID customerId,
                                                                            UUID projectId,
                                                                            UUID connectionId,
                                                                            CrmContainerType type,
                                                                            String remoteId);

    @Transactional
    void deleteByCustomerIdAndProjectIdAndConnection_Id(UUID customerId, UUID projectId, UUID connectionId);
}
