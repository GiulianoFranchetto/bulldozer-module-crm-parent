package com.bulldozer_collective.modules.crm.sync.data;

import com.bulldozer_collective.shared.IntegrationPlatform;
import com.bulldozer_collective.modules.crm.api.CrmSyncStatus;
import com.bulldozer_collective.shared.jpa.ProjectAwareId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface CrmSyncRunRepository extends JpaRepository<CrmSyncRunEntity, ProjectAwareId> {

    @Query("""
            SELECT r FROM CrmSyncRunEntity r
            WHERE r.customerId = :customerId
            AND r.projectId = :projectId
            AND (:platform IS NULL OR r.platform = :platform)
            AND (:status IS NULL OR r.status = :status)
            ORDER BY r.startedAt DESC
            """)
    Page<CrmSyncRunEntity> search(@Param("customerId") UUID customerId,
                                  @Param("projectId") UUID projectId,
                                  @Param("platform") IntegrationPlatform platform,
                                  @Param("status") CrmSyncStatus status,
                                  Pageable pageable);
}
