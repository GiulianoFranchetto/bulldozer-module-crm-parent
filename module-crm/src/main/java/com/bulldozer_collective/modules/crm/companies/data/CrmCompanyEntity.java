package com.bulldozer_collective.modules.crm.companies.data;

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

@Getter
@Setter
@Entity
@Table(
        name = "crm_companies",
        uniqueConstraints = @UniqueConstraint(columnNames = {"customerId", "projectId", "platform", "sourceId"})
)
public class CrmCompanyEntity extends ProjectAwareEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IntegrationPlatform platform;

    @Column(nullable = false)
    private String sourceId;

    @Column(nullable = false)
    private String name;

    @Nullable
    @Column
    private String domain;

    @Nullable
    @Column
    private String industry;

    @Nullable
    @Column
    private String country;

    @Nullable
    @Column
    private String city;
}
