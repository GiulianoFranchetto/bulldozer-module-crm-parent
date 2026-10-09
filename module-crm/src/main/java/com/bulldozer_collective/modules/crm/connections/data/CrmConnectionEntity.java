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
import org.jspecify.annotations.Nullable;

/**
 * A CRM connected to a project. The only CRM state the module keeps: which CRM, and how to reach it. The OAuth
 * token lives in the modulith; the CRM's data is never copied here.
 *
 * <p>At most one connection per platform and project: the modulith holds a single token per (project, platform).
 */
@Getter
@Setter
@Entity
@Table(
        name = "crm_connections",
        uniqueConstraints = @UniqueConstraint(columnNames = {"customer_id", "project_id", "platform"})
)
public class CrmConnectionEntity extends ProjectAwareEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IntegrationPlatform platform;

    /** The CRM's own API host when it is per tenant (Salesforce), {@code null} otherwise. */
    @Nullable
    @Column
    private String instanceUrl;
}
