package com.bulldozer_collective.modules.crm.imports.services;

import com.bulldozer_collective.companies.api.Company;
import com.bulldozer_collective.companies.api.clients.InternalGetCompaniesClient;
import com.bulldozer_collective.modules.outbound.lead_lists.Lead;
import com.bulldozer_collective.modules.outbound.lead_lists.LeadListEntry;
import com.bulldozer_collective.modules.outbound.lead_lists.LeadMembershipState;
import com.bulldozer_collective.modules.outbound.lead_lists.clients.InternalGetLeadListClient;
import com.bulldozer_collective.modules.outbound.lead_lists.clients.InternalListLeadListLeadsClient;
import com.bulldozer_collective.shared.exceptions.NotFoundException;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Reads a lead list and what its leads point to from the outbound and companies modules, over their internal routes. */
@Service
public class CrmLeadListSource {

    /** An import runs inside the caller's request: past this, the list is refused rather than half pushed. */
    static final int MAX_LEADS = 2_000;

    private static final int PAGE_SIZE = 100;

    /** @throws NotFoundException when the project has no such lead list. */
    public @NonNull String name(@NonNull UUID customerId, @NonNull UUID projectId, @NonNull UUID leadListId) {
        try {
            return InternalGetLeadListClient.builder()
                    .customerId(customerId)
                    .projectId(projectId)
                    .leadListId(leadListId)
                    .build()
                    .execute()
                    .leadList()
                    .name();
        } catch (HttpClientErrorException.NotFound e) {
            throw new NotFoundException("Lead list not found: " + leadListId);
        }
    }

    /**
     * The list's accepted leads, each with its contact or company inlined.
     *
     * @throws IllegalArgumentException when the list holds more than {@link #MAX_LEADS} accepted leads.
     */
    public @NonNull List<Lead> acceptedLeads(@NonNull UUID customerId,
                                             @NonNull UUID projectId,
                                             @NonNull UUID leadListId) {
        var leads = new ArrayList<Lead>();
        for (int page = 0; ; page++) {
            var response = InternalListLeadListLeadsClient.builder()
                    .customerId(customerId)
                    .projectId(projectId)
                    .leadListId(leadListId)
                    .state(LeadMembershipState.LEAD_MEMBERSHIP_STATE_ACCEPTED)
                    .withDetail(true)
                    .page(page)
                    .pageSize(PAGE_SIZE)
                    .build()
                    .execute();
            if (page == 0 && response.getTotalElement() > MAX_LEADS) {
                throw new IllegalArgumentException("leadListId: %d accepted leads, at most %d can be imported at once"
                        .formatted(response.getTotalElement(), MAX_LEADS));
            }
            var content = response.getContent() == null ? List.<LeadListEntry>of() : response.getContent();
            content.forEach(entry -> leads.add(entry.lead()));
            if (content.size() < PAGE_SIZE || leads.size() >= response.getTotalElement()) {
                return leads;
            }
        }
    }

    /** The customer's companies among {@code companyIds}; an unknown id is simply absent. */
    public @NonNull Map<UUID, Company> companies(@NonNull UUID customerId, @NonNull Collection<UUID> companyIds) {
        var ids = List.copyOf(companyIds);
        var companies = new HashMap<UUID, Company>();
        // The ids travel in the query string: by batches, to keep the URL short.
        for (int start = 0; start < ids.size(); start += PAGE_SIZE) {
            InternalGetCompaniesClient.builder()
                    .customerIds(List.of(customerId))
                    .companyIds(ids.subList(start, Math.min(start + PAGE_SIZE, ids.size())))
                    .build()
                    .execute()
                    .companies()
                    .forEach(company -> companies.put(company.id(), company));
        }
        return companies;
    }
}
