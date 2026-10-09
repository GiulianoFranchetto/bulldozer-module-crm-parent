package com.bulldozer_collective.modules.crm.stages.data;

import com.bulldozer_collective.shared.jpa.ProjectAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Which deal property gives the stage of a deal in one pipeline of a connection — HubSpot's native
 * {@code dealstage}, or a custom property the customer uses instead. Configuration, not CRM data.
 */
@Getter
@Setter
@Entity
@Table(
        name = "crm_stage_sources",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"customer_id", "project_id", "connection_id", "pipeline_id"})
)
public class CrmStageSourceEntity extends ProjectAwareEntity {

    @Column(nullable = false)
    private UUID connectionId;

    /** The CRM's own pipeline id. */
    @Column(nullable = false)
    private String pipelineId;

    @Column(nullable = false)
    private String propertyName;
}
