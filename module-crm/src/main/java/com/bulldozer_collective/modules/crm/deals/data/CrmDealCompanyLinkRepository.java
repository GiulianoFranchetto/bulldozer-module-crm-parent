package com.bulldozer_collective.modules.crm.deals.data;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CrmDealCompanyLinkRepository extends JpaRepository<CrmDealCompanyLinkEntity, UUID> {

    List<CrmDealCompanyLinkEntity> findAllByDealIdAndCustomerIdAndProjectId(UUID dealId, UUID customerId, UUID projectId);

    Optional<CrmDealCompanyLinkEntity> findByDealIdAndCompanyId(UUID dealId, UUID companyId);

    void deleteAllByDealIdAndCustomerIdAndProjectId(UUID dealId, UUID customerId, UUID projectId);

    void deleteAllByDealIdIn(java.util.Collection<UUID> dealIds);
}
