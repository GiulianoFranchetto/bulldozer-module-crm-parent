package com.bulldozer_collective.modules.crm.containers.data;

import com.bulldozer_collective.modules.crm.api.CrmContainerType;
import com.bulldozer_collective.modules.crm.connections.data.CrmConnectionEntity;
import com.bulldozer_collective.shared.jpa.ProjectAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/**
 * A CRM object grouping deals or leads and their stages — a HubSpot pipeline, a Salesforce sales or lead
 * process — imported by the project. Configuration, not CRM data: only its identity and the property its stages
 * are read from are kept.
 */
@Getter
@Setter
@Entity
@Table(
        name = "crm_containers",
        uniqueConstraints = @UniqueConstraint(columnNames = {
                "customer_id", "project_id", "ref_connection_id", "_type", "remote_id"})
)
public class CrmContainerEntity extends ProjectAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumns({
            @JoinColumn(name = "ref_connection_id", referencedColumnName = "id"),
            @JoinColumn(name = "ref_connection_customer_id", referencedColumnName = "customerId"),
            @JoinColumn(name = "ref_connection_project_id", referencedColumnName = "projectId"),
    })
    private CrmConnectionEntity connection;

    @Enumerated(EnumType.STRING)
    @Column(name = "_type", nullable = false)
    private CrmContainerType type;

    /** The CRM's own id of the pipeline / process. */
    @Column(nullable = false)
    private String remoteId;

    @Column(nullable = false)
    private String name;

    /** The enumerated property whose values are the container's stages. */
    @Column(nullable = false)
    private String stagePropertyName;
}
