package com.bulldozer_collective.modules.crm.connections.data;

import com.bulldozer_collective.shared.IntegrationPlatform;
import com.bulldozer_collective.shared.jpa.ProjectAwareId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CrmConnectionRepository extends JpaRepository<CrmConnectionEntity, ProjectAwareId> {

    List<CrmConnectionEntity> findAllByCustomerIdAndProjectId(UUID customerId, UUID projectId);

    Optional<CrmConnectionEntity> findByCustomerIdAndProjectIdAndPlatform(UUID customerId, UUID projectId, IntegrationPlatform platform);

    List<CrmConnectionEntity> findAllByDeletedAtIsNull();
}
