package com.bulldozer_collective.modules.crm.pipelines.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "crm_pipeline_stages")
public class CrmPipelineStageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID pipelineId;

    @Column(nullable = false)
    private String sourceId;

    @Column(nullable = false)
    private String label;

    @Column(nullable = false)
    private int displayOrder;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt = Instant.now();

    @UpdateTimestamp
    private Instant updatedAt = Instant.now();
}
