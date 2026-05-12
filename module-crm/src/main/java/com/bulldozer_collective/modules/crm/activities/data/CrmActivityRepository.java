package com.bulldozer_collective.modules.crm.activities.data;

import com.bulldozer_collective.modules.crm.api.CrmActivityType;
import com.bulldozer_collective.shared.IntegrationPlatform;
import com.bulldozer_collective.shared.jpa.ProjectAwareId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.jpa.repository.Modifying;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface CrmActivityRepository extends JpaRepository<CrmActivityEntity, ProjectAwareId> {

    @Query("""
            SELECT a FROM CrmActivityEntity a
            WHERE a.customerId = :customerId
            AND a.projectId = :projectId
            AND a.deletedAt IS NULL
            AND (:platform IS NULL OR a.platform = :platform)
            AND (:type IS NULL OR a.type = :type)
            AND (:dealId IS NULL OR a.dealId = :dealId)
            AND (:contactId IS NULL OR a.contactId = :contactId)
            AND (:companyId IS NULL OR a.companyId = :companyId)
            """)
    Page<CrmActivityEntity> search(@Param("customerId") UUID customerId,
                                   @Param("projectId") UUID projectId,
                                   @Param("platform") IntegrationPlatform platform,
                                   @Param("type") CrmActivityType type,
                                   @Param("dealId") UUID dealId,
                                   @Param("contactId") UUID contactId,
                                   @Param("companyId") UUID companyId,
                                   Pageable pageable);

    Optional<CrmActivityEntity> findByCustomerIdAndProjectIdAndPlatformAndSourceId(UUID customerId, UUID projectId, IntegrationPlatform platform, String sourceId);

    Optional<CrmActivityEntity> findByIdAndCustomerIdAndProjectIdAndDeletedAtIsNull(UUID id, UUID customerId, UUID projectId);

    @Modifying
    @Query("""
            UPDATE CrmActivityEntity a SET a.deletedAt = NULL
            WHERE a.customerId = :customerId
            AND a.projectId = :projectId
            AND a.platform = :platform
            AND a.sourceId IN :seenSourceIds
            AND a.deletedAt IS NOT NULL
            """)
    void reactivateSeen(@Param("customerId") UUID customerId,
                        @Param("projectId") UUID projectId,
                        @Param("platform") IntegrationPlatform platform,
                        @Param("seenSourceIds") Collection<String> seenSourceIds);

    @Modifying
    @Query("""
            UPDATE CrmActivityEntity a SET a.deletedAt = :deletedAt
            WHERE a.customerId = :customerId
            AND a.projectId = :projectId
            AND a.platform = :platform
            AND a.sourceId NOT IN :seenSourceIds
            AND a.deletedAt IS NULL
            """)
    void softDeleteMissing(@Param("customerId") UUID customerId,
                           @Param("projectId") UUID projectId,
                           @Param("platform") IntegrationPlatform platform,
                           @Param("seenSourceIds") Collection<String> seenSourceIds,
                           @Param("deletedAt") Instant deletedAt);
}
