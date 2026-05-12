package com.bulldozer_collective.modules.crm.pipelines.data;

import com.bulldozer_collective.modules.crm.api.CrmDealStage;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(
        name = "crm_deal_stage_mappings",
        uniqueConstraints = @UniqueConstraint(columnNames = {"pipelineStageId"})
)
public class CrmDealStageMappingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID pipelineStageId;

    @Column(nullable = false)
    private UUID customerId;

    @Column(nullable = false)
    private UUID projectId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CrmDealStage internalStage;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt = Instant.now();

    @UpdateTimestamp
    private Instant updatedAt = Instant.now();
}
