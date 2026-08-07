package com.bulldozer_collective.modules.crm.pipelines.data;

import com.bulldozer_collective.shared.jpa.ProjectAwareId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CrmPipelineStageRepository extends JpaRepository<CrmPipelineStageEntity, ProjectAwareId> {

    List<CrmPipelineStageEntity> findAllByPipelineIdAndDeletedAtIsNull(UUID pipelineId);

    List<CrmPipelineStageEntity> findAllByPipelineIdAndDeletedAtIsNullOrderByDisplayOrderAsc(UUID pipelineId);

    Optional<CrmPipelineStageEntity> findByPipelineIdAndSourceId(UUID pipelineId, String sourceId);

    /** Tenant-scoped lookup, for the one place a stage id arrives from the caller: stage-mapping creation. */
    Optional<CrmPipelineStageEntity> findByIdAndCustomerIdAndProjectIdAndDeletedAtIsNull(UUID id, UUID customerId, UUID projectId);

    /**
     * Clears the soft-delete of a stage that reappeared in the CRM.
     *
     * <p>This has to be a bulk update: {@code ProjectAwareEntity.deletedAt} is {@code @Column(updatable =
     * false)}, so Hibernate leaves it out of every UPDATE it generates from entity state — a
     * {@code setDeletedAt(null)} followed by {@code save()} is silently a no-op.
     */
    @Transactional
    @Modifying
    @Query("""
            UPDATE CrmPipelineStageEntity s SET s.deletedAt = NULL
            WHERE s.pipelineId = :pipelineId
            AND s.sourceId = :sourceId
            AND s.deletedAt IS NOT NULL
            """)
    void reactivateBySourceId(@Param("pipelineId") UUID pipelineId,
                              @Param("sourceId") String sourceId);

    /**
     * Soft-deletes the stages of a pipeline that the platform no longer returns. Without this, a stage
     * removed in the CRM keeps demanding a deal-stage mapping and blocks every later sync for good.
     */
    @Transactional
    @Modifying
    @Query("""
            UPDATE CrmPipelineStageEntity s SET s.deletedAt = :deletedAt
            WHERE s.pipelineId = :pipelineId
            AND s.sourceId NOT IN :seenSourceIds
            AND s.deletedAt IS NULL
            """)
    void softDeleteMissing(@Param("pipelineId") UUID pipelineId,
                           @Param("seenSourceIds") Collection<String> seenSourceIds,
                           @Param("deletedAt") Instant deletedAt);
}
