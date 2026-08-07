package com.bulldozer_collective.modules.crm.deals.data;

import com.bulldozer_collective.modules.crm.api.CrmDealStage;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CrmDealRepository extends JpaRepository<CrmDealEntity, ProjectAwareId> {

    @Query("""
            SELECT d FROM CrmDealEntity d
            WHERE d.customerId = :customerId
            AND d.projectId = :projectId
            AND d.deletedAt IS NULL
            AND (:platform IS NULL OR d.platform = :platform)
            AND (:internalStage IS NULL OR d.internalStage = :internalStage)
            AND (:pipelineId IS NULL OR d.pipelineId = :pipelineId)
            """)
    Page<CrmDealEntity> search(@Param("customerId") UUID customerId,
                               @Param("projectId") UUID projectId,
                               @Param("platform") IntegrationPlatform platform,
                               @Param("internalStage") CrmDealStage internalStage,
                               @Param("pipelineId") UUID pipelineId,
                               Pageable pageable);

    Optional<CrmDealEntity> findByCustomerIdAndProjectIdAndPlatformAndSourceId(UUID customerId, UUID projectId, IntegrationPlatform platform, String sourceId);

    Optional<CrmDealEntity> findByIdAndCustomerIdAndProjectIdAndDeletedAtIsNull(UUID id, UUID customerId, UUID projectId);

    List<CrmDealEntity> findAllByCustomerIdAndProjectIdAndPipelineStageIdAndDeletedAtIsNull(UUID customerId, UUID projectId, UUID pipelineStageId);

    List<CrmDealEntity> findAllByCustomerIdAndProjectIdAndPlatformAndDeletedAtIsNull(UUID customerId, UUID projectId, IntegrationPlatform platform);

    /**
     * Batch load for the upsert path: one query per CRM page instead of one per object.
     */
    List<CrmDealEntity> findAllByCustomerIdAndProjectIdAndPlatformAndSourceIdIn(UUID customerId, UUID projectId, IntegrationPlatform platform, Collection<String> sourceIds);

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
            UPDATE CrmDealEntity d SET d.deletedAt = NULL
            WHERE d.customerId = :customerId
            AND d.projectId = :projectId
            AND d.platform = :platform
            AND d.lastSeenAt >= :passStartedAt
            AND d.deletedAt IS NOT NULL
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
            UPDATE CrmDealEntity d SET d.deletedAt = :deletedAt
            WHERE d.customerId = :customerId
            AND d.projectId = :projectId
            AND d.platform = :platform
            AND (d.lastSeenAt IS NULL OR d.lastSeenAt < :passStartedAt)
            AND d.deletedAt IS NULL
            """)
    void softDeleteNotSeenSince(@Param("customerId") UUID customerId,
                                @Param("projectId") UUID projectId,
                                @Param("platform") IntegrationPlatform platform,
                                @Param("passStartedAt") Instant passStartedAt,
                                @Param("deletedAt") Instant deletedAt);
}
