package com.bulldozer_collective.modules.crm.connections.data;

import com.bulldozer_collective.shared.IntegrationPlatform;
import com.bulldozer_collective.shared.jpa.ProjectAwareId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CrmConnectionRepository extends JpaRepository<CrmConnectionEntity, ProjectAwareId> {

    List<CrmConnectionEntity> findAllByCustomerIdAndProjectIdOrderByCreatedAtAsc(UUID customerId, UUID projectId);

    Optional<CrmConnectionEntity> findByCustomerIdAndProjectIdAndId(UUID customerId, UUID projectId, UUID id);

    boolean existsByCustomerIdAndProjectIdAndPlatform(UUID customerId, UUID projectId, IntegrationPlatform platform);

    @Transactional
    void deleteByCustomerIdAndProjectIdAndId(UUID customerId, UUID projectId, UUID id);
}
