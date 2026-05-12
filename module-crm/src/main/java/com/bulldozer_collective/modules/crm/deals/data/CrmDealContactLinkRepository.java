package com.bulldozer_collective.modules.crm.deals.data;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CrmDealContactLinkRepository extends JpaRepository<CrmDealContactLinkEntity, UUID> {

    List<CrmDealContactLinkEntity> findAllByDealIdAndCustomerIdAndProjectId(UUID dealId, UUID customerId, UUID projectId);

    Optional<CrmDealContactLinkEntity> findByDealIdAndContactId(UUID dealId, UUID contactId);

    void deleteAllByDealIdAndCustomerIdAndProjectId(UUID dealId, UUID customerId, UUID projectId);

    void deleteAllByDealIdIn(java.util.Collection<UUID> dealIds);
}
