package com.bulldozer_collective.modules.crm.associations.data;

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
        name = "crm_contact_company_links",
        uniqueConstraints = @UniqueConstraint(columnNames = {"contactId", "companyId"})
)
public class CrmContactCompanyLinkEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID contactId;

    @Column(nullable = false)
    private UUID companyId;

    @Column(nullable = false)
    private UUID customerId;

    @Column(nullable = false)
    private UUID projectId;
}
