package com.bulldozer_collective.modules.crm.deals.data;

import com.bulldozer_collective.shared.jpa.ProjectAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(
        name = "crm_deal_company_links",
        uniqueConstraints = @UniqueConstraint(columnNames = {"deal_id", "company_id"})
)
public class CrmDealCompanyLinkEntity extends ProjectAwareEntity {

    @Column(nullable = false)
    private UUID dealId;

    @Column(nullable = false)
    private UUID companyId;

}
