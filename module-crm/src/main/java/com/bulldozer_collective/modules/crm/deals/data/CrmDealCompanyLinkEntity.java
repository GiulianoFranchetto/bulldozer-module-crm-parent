package com.bulldozer_collective.modules.crm.deals.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
        uniqueConstraints = @UniqueConstraint(columnNames = {"dealId", "companyId"})
)
public class CrmDealCompanyLinkEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID dealId;

    @Column(nullable = false)
    private UUID companyId;

    @Column(nullable = false)
    private UUID customerId;

    @Column(nullable = false)
    private UUID projectId;
}
