package com.bulldozer_collective.modules.crm.pipelines.data;

import com.bulldozer_collective.shared.IntegrationPlatform;
import com.bulldozer_collective.shared.jpa.ProjectAwareId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CrmPipelineRepository extends JpaRepository<CrmPipelineEntity, ProjectAwareId> {

    List<CrmPipelineEntity> findAllByCustomerIdAndProjectIdAndDeletedAtIsNull(UUID customerId, UUID projectId);

    List<CrmPipelineEntity> findAllByCustomerIdAndProjectIdAndPlatformAndDeletedAtIsNull(UUID customerId, UUID projectId, IntegrationPlatform platform);

    Optional<CrmPipelineEntity> findByCustomerIdAndProjectIdAndPlatformAndSourceId(UUID customerId, UUID projectId, IntegrationPlatform platform, String sourceId);

    @Modifying
    @Query("""
            UPDATE CrmPipelineEntity p SET p.deletedAt = NULL
            WHERE p.customerId = :customerId
            AND p.projectId = :projectId
            AND p.platform = :platform
            AND p.sourceId = :sourceId
            AND p.deletedAt IS NOT NULL
            """)
    void reactivateBySourceId(@Param("customerId") UUID customerId,
                              @Param("projectId") UUID projectId,
                              @Param("platform") IntegrationPlatform platform,
                              @Param("sourceId") String sourceId);
}
