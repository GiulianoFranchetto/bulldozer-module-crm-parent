package com.bulldozer_collective.modules.crm.stages.data;

import com.bulldozer_collective.modules.crm.api.CrmDealStage;
import com.bulldozer_collective.shared.jpa.ProjectAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * "In this pipeline, the stage source value {@code value} means {@code stage}." Several values may map to the
 * same stage; {@code writeDefault} marks the one written when a deal is set to that stage. Configuration, not
 * CRM data.
 */
@Getter
@Setter
@Entity
@Table(
        name = "crm_stage_mappings",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"customer_id", "project_id", "connection_id", "pipeline_id", "_value"})
)
public class CrmStageMappingEntity extends ProjectAwareEntity {

    @Column(nullable = false)
    private UUID connectionId;

    /** The CRM's own pipeline id. */
    @Column(nullable = false)
    private String pipelineId;

    /** A value of the pipeline's stage source property, as the CRM stores it. */
    @Column(name = "_value", nullable = false)
    private String value;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CrmDealStage stage;

    @Column(nullable = false)
    private boolean writeDefault;
}
