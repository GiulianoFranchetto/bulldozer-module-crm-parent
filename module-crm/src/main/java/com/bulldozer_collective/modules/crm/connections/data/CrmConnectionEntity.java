package com.bulldozer_collective.modules.crm.connections.data;

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

@Getter
@Setter
@Entity
@Table(
        name = "crm_connections",
        uniqueConstraints = @UniqueConstraint(columnNames = {"customerId", "projectId", "platform"})
)
public class CrmConnectionEntity extends ProjectAwareEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IntegrationPlatform platform;
}
