package com.bulldozer_collective.modules.crm.associations.services;

import com.bulldozer_collective.modules.crm.associations.data.CrmContactCompanyLinkEntity;
import com.bulldozer_collective.modules.crm.associations.data.CrmContactCompanyLinkRepository;
import com.bulldozer_collective.modules.crm.deals.data.CrmDealCompanyLinkEntity;
import com.bulldozer_collective.modules.crm.deals.data.CrmDealCompanyLinkRepository;
import com.bulldozer_collective.modules.crm.deals.data.CrmDealContactLinkEntity;
import com.bulldozer_collective.modules.crm.deals.data.CrmDealContactLinkRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Replaces association rows for a bounded batch of owners, atomically.
 *
 * <p>Atomicity is the point. The CRM is the source of truth for associations, so a sync replaces rather than
 * merges — and delete-then-insert across two transactions means a failure in between leaves the links deleted
 * and not re-created, so every association of that batch is lost until the next successful sync. Each method
 * here does both halves in one transaction.
 *
 * <p>Callers must batch: the delete is keyed on an {@code IN} list of owner ids, and a portal holds far more
 * deals than PostgreSQL accepts bind parameters for. {@link #MAX_OWNERS_PER_BATCH} is that bound.
 */
@Service
@RequiredArgsConstructor
public class CrmAssociationPersister {

    /**
     * Owners per transaction. Well under PostgreSQL's 32767-parameter ceiling, and small enough that a failure
     * costs one batch rather than the whole portal.
     */
    public static final int MAX_OWNERS_PER_BATCH = 500;

    private final CrmContactCompanyLinkRepository contactCompanyLinkRepository;
    private final CrmDealContactLinkRepository dealContactLinkRepository;
    private final CrmDealCompanyLinkRepository dealCompanyLinkRepository;

    @Transactional
    public void replaceContactCompanyLinks(@NonNull ReplaceCrmLinksBuilder request) {
        assertBatchSize(request.ownerIds());
        this.contactCompanyLinkRepository.deleteAllByContactIdIn(request.ownerIds());

        var links = new ArrayList<CrmContactCompanyLinkEntity>();
        for (var contactId : request.ownerIds()) {
            for (var companyId : request.targetsByOwner().getOrDefault(contactId, List.of())) {
                var link = new CrmContactCompanyLinkEntity();
                link.setCustomerId(request.customerId());
                link.setProjectId(request.projectId());
                link.setContactId(contactId);
                link.setCompanyId(companyId);
                links.add(link);
            }
        }
        this.contactCompanyLinkRepository.saveAll(links);
    }

    @Transactional
    public void replaceDealContactLinks(@NonNull ReplaceCrmLinksBuilder request) {
        assertBatchSize(request.ownerIds());
        this.dealContactLinkRepository.deleteAllByDealIdIn(request.ownerIds());

        var links = new ArrayList<CrmDealContactLinkEntity>();
        for (var dealId : request.ownerIds()) {
            for (var contactId : request.targetsByOwner().getOrDefault(dealId, List.of())) {
                var link = new CrmDealContactLinkEntity();
                link.setCustomerId(request.customerId());
                link.setProjectId(request.projectId());
                link.setDealId(dealId);
                link.setContactId(contactId);
                links.add(link);
            }
        }
        this.dealContactLinkRepository.saveAll(links);
    }

    @Transactional
    public void replaceDealCompanyLinks(@NonNull ReplaceCrmLinksBuilder request) {
        assertBatchSize(request.ownerIds());
        this.dealCompanyLinkRepository.deleteAllByDealIdIn(request.ownerIds());

        var links = new ArrayList<CrmDealCompanyLinkEntity>();
        for (var dealId : request.ownerIds()) {
            for (var companyId : request.targetsByOwner().getOrDefault(dealId, List.of())) {
                var link = new CrmDealCompanyLinkEntity();
                link.setCustomerId(request.customerId());
                link.setProjectId(request.projectId());
                link.setDealId(dealId);
                link.setCompanyId(companyId);
                links.add(link);
            }
        }
        this.dealCompanyLinkRepository.saveAll(links);
    }

    private static void assertBatchSize(@NonNull Collection<UUID> owners) {
        if (owners.size() > MAX_OWNERS_PER_BATCH) {
            throw new IllegalArgumentException(
                    "Association batches are capped at %d owners, got %d".formatted(MAX_OWNERS_PER_BATCH, owners.size()));
        }
    }
}
