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
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.List;
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

    /**
     * Batch load for the upsert path: one query per CRM page instead of one per object.
     */
    List<CrmActivityEntity> findAllByCustomerIdAndProjectIdAndPlatformAndSourceIdIn(UUID customerId, UUID projectId, IntegrationPlatform platform, Collection<String> sourceIds);

    /**
     * Clears the soft-delete of every record this pass saw again.
     *
     * <p>A bulk update rather than entity state: {@code ProjectAwareEntity.deletedAt} is
     * {@code @Column(updatable = false)}, so Hibernate leaves it out of the UPDATEs it derives from an entity
     * and {@code setDeletedAt(null)} + {@code save()} is silently a no-op.
     */
    @Transactional
    @Modifying
    @Query("""
            UPDATE CrmActivityEntity a SET a.deletedAt = NULL
            WHERE a.customerId = :customerId
            AND a.projectId = :projectId
            AND a.platform = :platform
            AND a.lastSeenAt >= :passStartedAt
            AND a.deletedAt IS NOT NULL
            """)
    void reactivateSeenSince(@Param("customerId") UUID customerId,
                             @Param("projectId") UUID projectId,
                             @Param("platform") IntegrationPlatform platform,
                             @Param("passStartedAt") Instant passStartedAt);

    /**
     * Soft-deletes whatever the pass did not see. Constant number of bind parameters, unlike the
     * {@code sourceId NOT IN (:seen)} it replaces.
     */
    @Transactional
    @Modifying
    @Query("""
            UPDATE CrmActivityEntity a SET a.deletedAt = :deletedAt
            WHERE a.customerId = :customerId
            AND a.projectId = :projectId
            AND a.platform = :platform
            AND (a.lastSeenAt IS NULL OR a.lastSeenAt < :passStartedAt)
            AND a.deletedAt IS NULL
            """)
    void softDeleteNotSeenSince(@Param("customerId") UUID customerId,
                                @Param("projectId") UUID projectId,
                                @Param("platform") IntegrationPlatform platform,
                                @Param("passStartedAt") Instant passStartedAt,
                                @Param("deletedAt") Instant deletedAt);
}
