package com.bulldozer_collective.modules.crm.contacts.data;

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
        name = "crm_contacts",
        uniqueConstraints = @UniqueConstraint(columnNames = {"customerId", "projectId", "platform", "sourceId"})
)
public class CrmContactEntity extends ProjectAwareEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IntegrationPlatform platform;

    @Column(nullable = false)
    private String sourceId;

    @Nullable
    @Column
    private String firstName;

    @Nullable
    @Column
    private String lastName;

    @Nullable
    @Column
    private String email;

    @Nullable
    @Column
    private String phone;

    @Nullable
    @Column
    private String jobTitle;

    @Nullable
    @Column
    private String linkedInUrl;
}
