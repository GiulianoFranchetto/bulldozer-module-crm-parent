package com.bulldozer_collective.modules.crm.companies.data;

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

public interface CrmCompanyRepository extends JpaRepository<CrmCompanyEntity, ProjectAwareId> {

    @Query("""
            SELECT c FROM CrmCompanyEntity c
            WHERE c.customerId = :customerId
            AND c.projectId = :projectId
            AND c.deletedAt IS NULL
            AND (:platform IS NULL OR c.platform = :platform)
            AND (:q IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :q, '%'))
                           OR LOWER(c.domain) LIKE LOWER(CONCAT('%', :q, '%')))
            """)
    Page<CrmCompanyEntity> search(@Param("customerId") UUID customerId,
                                  @Param("projectId") UUID projectId,
                                  @Param("platform") IntegrationPlatform platform,
                                  @Param("q") String q,
                                  Pageable pageable);

    Optional<CrmCompanyEntity> findByCustomerIdAndProjectIdAndPlatformAndSourceId(UUID customerId, UUID projectId, IntegrationPlatform platform, String sourceId);

    Optional<CrmCompanyEntity> findByIdAndCustomerIdAndProjectIdAndDeletedAtIsNull(UUID id, UUID customerId, UUID projectId);

    @Modifying
    @Query("""
            UPDATE CrmCompanyEntity c SET c.deletedAt = NULL
            WHERE c.customerId = :customerId
            AND c.projectId = :projectId
            AND c.platform = :platform
            AND c.sourceId IN :seenSourceIds
            AND c.deletedAt IS NOT NULL
            """)
    void reactivateSeen(@Param("customerId") UUID customerId,
                        @Param("projectId") UUID projectId,
                        @Param("platform") IntegrationPlatform platform,
                        @Param("seenSourceIds") Collection<String> seenSourceIds);

    @Modifying
    @Query("""
            UPDATE CrmCompanyEntity c SET c.deletedAt = :deletedAt
            WHERE c.customerId = :customerId
            AND c.projectId = :projectId
            AND c.platform = :platform
            AND c.sourceId NOT IN :seenSourceIds
            AND c.deletedAt IS NULL
            """)
    void softDeleteMissing(@Param("customerId") UUID customerId,
                           @Param("projectId") UUID projectId,
                           @Param("platform") IntegrationPlatform platform,
                           @Param("seenSourceIds") Collection<String> seenSourceIds,
                           @Param("deletedAt") Instant deletedAt);
}
