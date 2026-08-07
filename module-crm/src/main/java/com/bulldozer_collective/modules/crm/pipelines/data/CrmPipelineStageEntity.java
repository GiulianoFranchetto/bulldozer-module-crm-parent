package com.bulldozer_collective.modules.crm.pipelines.data;

import com.bulldozer_collective.shared.jpa.ProjectAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(
        name = "crm_pipeline_stages",
        // Re-discovering a pipeline must update its stages, never duplicate them. CrmPipelineUpsertService
        // relies on this couple; without the constraint two concurrent discoveries insert twin rows.
        uniqueConstraints = @UniqueConstraint(columnNames = {"pipeline_id", "source_id"})
)
public class CrmPipelineStageEntity extends ProjectAwareEntity {

    @Column(nullable = false)
    private UUID pipelineId;

    @Column(nullable = false)
    private String sourceId;

    @Column(nullable = false)
    private String label;

    @Column(nullable = false)
    private int displayOrder;
}
