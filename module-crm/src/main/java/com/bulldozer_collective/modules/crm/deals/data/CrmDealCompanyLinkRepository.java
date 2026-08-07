package com.bulldozer_collective.modules.crm.deals.data;

import com.bulldozer_collective.shared.jpa.ProjectAwareId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CrmDealCompanyLinkRepository extends JpaRepository<CrmDealCompanyLinkEntity, ProjectAwareId> {

    List<CrmDealCompanyLinkEntity> findAllByDealIdAndCustomerIdAndProjectId(UUID dealId, UUID customerId, UUID projectId);

    Optional<CrmDealCompanyLinkEntity> findByDealIdAndCompanyId(UUID dealId, UUID companyId);

    @Transactional

    void deleteAllByDealIdAndCustomerIdAndProjectId(UUID dealId, UUID customerId, UUID projectId);

    @Transactional

    void deleteAllByDealIdIn(java.util.Collection<UUID> dealIds);
}
