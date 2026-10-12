package com.bulldozer_collective.modules.crm.containers.data;

import com.bulldozer_collective.modules.crm.api.CrmDealStage;
import com.bulldozer_collective.modules.crm.api.CrmLeadStage;
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
import org.jspecify.annotations.Nullable;

/**
 * One value of a container's stage source property, with the Bulldozer stage it is mapped to — {@code dealStage}
 * in a deal container, {@code leadStage} in a lead container, both {@code null} while unmapped. When several
 * values map to the same stage, {@code writeDefault} marks the one written when a record is set to that stage.
 */
@Getter
@Setter
@Entity
@Table(
        name = "crm_container_stages",
        uniqueConstraints = @UniqueConstraint(columnNames = {
                "ref_container_id", "ref_container_customer_id", "ref_container_project_id", "remote_id"})
)
public class CrmContainerStageEntity extends ProjectAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumns({
            @JoinColumn(name = "ref_container_id", referencedColumnName = "id"),
            @JoinColumn(name = "ref_container_customer_id", referencedColumnName = "customerId"),
            @JoinColumn(name = "ref_container_project_id", referencedColumnName = "projectId"),
    })
    private CrmContainerEntity container;

    /** The value as the CRM stores it in the stage source property. */
    @Column(nullable = false)
    private String remoteId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int displayOrder;

    @Nullable
    @Enumerated(EnumType.STRING)
    @Column
    private CrmDealStage dealStage;

    @Nullable
    @Enumerated(EnumType.STRING)
    @Column
    private CrmLeadStage leadStage;

    @Column(nullable = false)
    private boolean writeDefault;
}
