package com.bulldozer_collective.modules.crm.associations.data;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CrmContactCompanyLinkRepository extends JpaRepository<CrmContactCompanyLinkEntity, UUID> {

    List<CrmContactCompanyLinkEntity> findAllByContactIdAndCustomerIdAndProjectId(UUID contactId, UUID customerId, UUID projectId);

    List<CrmContactCompanyLinkEntity> findAllByCompanyIdAndCustomerIdAndProjectId(UUID companyId, UUID customerId, UUID projectId);

    Optional<CrmContactCompanyLinkEntity> findByContactIdAndCompanyId(UUID contactId, UUID companyId);

    void deleteAllByContactIdIn(java.util.Collection<UUID> contactIds);
}
