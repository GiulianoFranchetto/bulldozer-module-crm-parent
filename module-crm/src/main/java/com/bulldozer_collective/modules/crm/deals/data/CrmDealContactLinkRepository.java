package com.bulldozer_collective.modules.crm.deals.data;

import com.bulldozer_collective.shared.jpa.ProjectAwareId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CrmDealContactLinkRepository extends JpaRepository<CrmDealContactLinkEntity, ProjectAwareId> {

    List<CrmDealContactLinkEntity> findAllByDealIdAndCustomerIdAndProjectId(UUID dealId, UUID customerId, UUID projectId);

    Optional<CrmDealContactLinkEntity> findByDealIdAndContactId(UUID dealId, UUID contactId);

    @Transactional

    void deleteAllByDealIdAndCustomerIdAndProjectId(UUID dealId, UUID customerId, UUID projectId);

    @Transactional

    void deleteAllByDealIdIn(java.util.Collection<UUID> dealIds);
}
