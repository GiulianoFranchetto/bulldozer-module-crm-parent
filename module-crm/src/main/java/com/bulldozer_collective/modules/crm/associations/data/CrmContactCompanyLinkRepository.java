package com.bulldozer_collective.modules.crm.associations.data;

import com.bulldozer_collective.shared.jpa.ProjectAwareId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CrmContactCompanyLinkRepository extends JpaRepository<CrmContactCompanyLinkEntity, ProjectAwareId> {

    /** Ordered, so a contact associated with several companies resolves to the same employer every run. */
    List<CrmContactCompanyLinkEntity> findAllByContactIdAndCustomerIdAndProjectIdOrderByCreatedAtAsc(UUID contactId, UUID customerId, UUID projectId);

    List<CrmContactCompanyLinkEntity> findAllByCompanyIdAndCustomerIdAndProjectId(UUID companyId, UUID customerId, UUID projectId);

    Optional<CrmContactCompanyLinkEntity> findByContactIdAndCompanyId(UUID contactId, UUID companyId);

    @Transactional

    void deleteAllByContactIdIn(java.util.Collection<UUID> contactIds);
}
