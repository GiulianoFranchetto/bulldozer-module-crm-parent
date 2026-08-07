package com.bulldozer_collective.modules.crm.pipelines.data;

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

@Getter
@Setter
@Entity
@Table(
        name = "crm_deal_stage_mappings",
        uniqueConstraints = @UniqueConstraint(columnNames = {"pipeline_stage_id"})
)
public class CrmDealStageMappingEntity extends ProjectAwareEntity {

    @Column(nullable = false)
    private UUID pipelineStageId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CrmDealStage internalStage;
}
