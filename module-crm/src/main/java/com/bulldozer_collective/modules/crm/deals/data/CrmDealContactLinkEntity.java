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
        name = "crm_deal_contact_links",
        uniqueConstraints = @UniqueConstraint(columnNames = {"dealId", "contactId"})
)
public class CrmDealContactLinkEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID dealId;

    @Column(nullable = false)
    private UUID contactId;

    @Column(nullable = false)
    private UUID customerId;

    @Column(nullable = false)
    private UUID projectId;
}
