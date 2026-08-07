package com.bulldozer_collective.modules.crm.contacts.data;

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

public interface CrmContactRepository extends JpaRepository<CrmContactEntity, ProjectAwareId> {

    @Query("""
            SELECT c FROM CrmContactEntity c
            WHERE c.customerId = :customerId
            AND c.projectId = :projectId
            AND c.deletedAt IS NULL
            AND (:platform IS NULL OR c.platform = :platform)
            AND (:q IS NULL OR LOWER(c.firstName) LIKE LOWER(CONCAT('%', :q, '%'))
                           OR LOWER(c.lastName) LIKE LOWER(CONCAT('%', :q, '%'))
                           OR LOWER(c.email) LIKE LOWER(CONCAT('%', :q, '%')))
            """)
    Page<CrmContactEntity> search(@Param("customerId") UUID customerId,
                                  @Param("projectId") UUID projectId,
                                  @Param("platform") IntegrationPlatform platform,
                                  @Param("q") String q,
                                  Pageable pageable);

    Optional<CrmContactEntity> findByCustomerIdAndProjectIdAndPlatformAndSourceId(UUID customerId, UUID projectId, IntegrationPlatform platform, String sourceId);

    List<CrmContactEntity> findAllByCustomerIdAndProjectIdAndPlatformAndDeletedAtIsNull(UUID customerId, UUID projectId, IntegrationPlatform platform);

    /**
     * Mirror rows not yet resolved to a canonical contact — the input of the linking step, bounded by the
     * caller's {@code Pageable}.
     *
     * <p>Nameless rows are excluded rather than returned and skipped: the contacts module requires a first and
     * a last name, so they can never resolve, and returning them would make the linking task reschedule itself
     * forever over rows it can do nothing with.
     */
    @Query("""
            SELECT c FROM CrmContactEntity c
            WHERE c.customerId = :customerId
            AND c.projectId = :projectId
            AND c.platform = :platform
            AND c.contactId IS NULL
            AND (c.firstName IS NOT NULL OR c.lastName IS NOT NULL)
            AND c.deletedAt IS NULL
            ORDER BY c.createdAt
            """)
    List<CrmContactEntity> findLinkable(@Param("customerId") UUID customerId,
                                        @Param("projectId") UUID projectId,
                                        @Param("platform") IntegrationPlatform platform,
                                        Pageable pageable);

    Optional<CrmContactEntity> findByIdAndCustomerIdAndProjectIdAndDeletedAtIsNull(UUID id, UUID customerId, UUID projectId);

    /**
     * Batch load for the upsert path: one query per CRM page instead of one per object.
     */
    List<CrmContactEntity> findAllByCustomerIdAndProjectIdAndPlatformAndSourceIdIn(UUID customerId, UUID projectId, IntegrationPlatform platform, Collection<String> sourceIds);

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
            UPDATE CrmContactEntity c SET c.deletedAt = NULL
            WHERE c.customerId = :customerId
            AND c.projectId = :projectId
            AND c.platform = :platform
            AND c.lastSeenAt >= :passStartedAt
            AND c.deletedAt IS NOT NULL
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
            UPDATE CrmContactEntity c SET c.deletedAt = :deletedAt
            WHERE c.customerId = :customerId
            AND c.projectId = :projectId
            AND c.platform = :platform
            AND (c.lastSeenAt IS NULL OR c.lastSeenAt < :passStartedAt)
            AND c.deletedAt IS NULL
            """)
    void softDeleteNotSeenSince(@Param("customerId") UUID customerId,
                                @Param("projectId") UUID projectId,
                                @Param("platform") IntegrationPlatform platform,
                                @Param("passStartedAt") Instant passStartedAt,
                                @Param("deletedAt") Instant deletedAt);
}
