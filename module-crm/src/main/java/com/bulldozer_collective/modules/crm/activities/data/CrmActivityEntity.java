package com.bulldozer_collective.modules.crm.activities.data;

import com.bulldozer_collective.modules.crm.api.CrmActivityType;
import com.bulldozer_collective.shared.IntegrationPlatform;
import com.bulldozer_collective.shared.jpa.ProjectAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(
        name = "crm_activities",
        uniqueConstraints = @UniqueConstraint(columnNames = {"customerId", "projectId", "platform", "sourceId"})
)
public class CrmActivityEntity extends ProjectAwareEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IntegrationPlatform platform;

    @Column(nullable = false)
    private String sourceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CrmActivityType type;

    @Nullable
    @Column
    private String title;

    @Nullable
    @Column(columnDefinition = "TEXT")
    private String body;

    @Nullable
    @Column
    private Instant dueDate;

    @Nullable
    @Column
    private Instant completedAt;

    @Nullable
    @Column
    private UUID dealId;

    @Nullable
    @Column
    private UUID contactId;

    @Nullable
    @Column
    private UUID companyId;
}
