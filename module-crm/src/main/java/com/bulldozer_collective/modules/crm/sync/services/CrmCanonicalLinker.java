package com.bulldozer_collective.modules.crm.sync.services;

import com.bulldozer_collective.logging.BulldozerLogger;
import com.bulldozer_collective.modules.crm.associations.data.CrmContactCompanyLinkRepository;
import com.bulldozer_collective.modules.crm.companies.data.CrmCompanyRepository;
import com.bulldozer_collective.modules.crm.companies.services.CrmCompanyResolver;
import com.bulldozer_collective.modules.crm.contacts.data.CrmContactEntity;
import com.bulldozer_collective.modules.crm.contacts.data.CrmContactRepository;
import com.bulldozer_collective.modules.crm.contacts.services.CrmContactResolver;
import com.bulldozer_collective.shared.IntegrationPlatform;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.bulldozer_collective.modules.crm.configurations.CrmLogContext.op;

/**
 * Attaches mirrored CRM contacts and companies to their canonical Bulldozer counterparts — the pattern the rest
 * of the fleet uses for mirrored external objects ({@code LinkedInEmployeeEntity.contactId},
 * {@code GbpLocationEntity.companyId}, {@code SignalLeadEntity.leadId}): the mirror row keeps the provider's own
 * id as its dedup key and gains a nullable id pointing at the canonical entity.
 *
 * <p>Runs after the objects and their associations are synced: a contact's company comes from the
 * contact ↔ company links, so those must exist first. Companies are resolved before contacts so a newly created
 * contact can carry its {@code companyId} straight away.
 *
 * <p><b>Bounded per call.</b> Every resolution is one or two HTTP calls to the contacts / companies module, so a
 * portal with tens of thousands of rows cannot be linked in a single job task — it would hold a scheduler thread
 * for hours with no way to resume. {@link #linkBatch} does at most {@link #MAX_ROWS_PER_BATCH} rows and reports
 * whether more remain; the task reschedules itself while progress is being made.
 *
 * <p>Not transactional: the resolutions are remote calls. Rows are saved one at a time on purpose — a failure
 * midway keeps everything already linked, and the next batch picks up only what is still unlinked.
 */
@Service
@RequiredArgsConstructor
public class CrmCanonicalLinker {

    /**
     * Rows per call. Sized so one batch is a few hundred internal HTTP calls — seconds of work, not hours.
     */
    public static final int MAX_ROWS_PER_BATCH = 200;

    private final CrmContactRepository contactRepository;
    private final CrmCompanyRepository companyRepository;
    private final CrmContactCompanyLinkRepository contactCompanyLinkRepository;
    private final CrmContactResolver contactResolver;
    private final CrmCompanyResolver companyResolver;
    private final BulldozerLogger logger;

    /**
     * Links one batch: companies first, and contacts only with whatever budget is left, so a contact is never
     * linked before the company it works for.
     */
    public CrmLinkProgress linkBatch(@NonNull UUID customerId,
                                     @NonNull UUID projectId,
                                     @NonNull IntegrationPlatform platform) {
        var linkedCompanies = this.linkCompanies(customerId, projectId, platform, MAX_ROWS_PER_BATCH);
        var linkedContacts = linkedCompanies < MAX_ROWS_PER_BATCH
                ? this.linkContacts(customerId, projectId, platform, MAX_ROWS_PER_BATCH - linkedCompanies)
                : 0;

        var linked = linkedCompanies + linkedContacts;
        var moreToDo = !this.companyRepository
                .findLinkable(customerId, projectId, platform, PageRequest.of(0, 1)).isEmpty()
                || !this.contactRepository
                .findLinkable(customerId, projectId, platform, PageRequest.of(0, 1)).isEmpty();

        this.logger.info(op("CRM-004", customerId, projectId)
                        + "Linked %d company(ies) and %d contact(s) on platform %s (more to do: %s)",
                linkedCompanies, linkedContacts, platform, moreToDo);

        return new CrmLinkProgress(linked, moreToDo);
    }

    private int linkCompanies(@NonNull UUID customerId,
                              @NonNull UUID projectId,
                              @NonNull IntegrationPlatform platform,
                              int budget) {
        var linked = 0;
        for (var company : this.companyRepository
                .findLinkable(customerId, projectId, platform, PageRequest.of(0, budget))) {
            var companyId = this.companyResolver.resolve(customerId, projectId, company);
            if (companyId == null) {
                continue;
            }
            company.setCompanyId(companyId);
            this.companyRepository.save(company);
            linked++;
        }
        return linked;
    }

    private int linkContacts(@NonNull UUID customerId,
                             @NonNull UUID projectId,
                             @NonNull IntegrationPlatform platform,
                             int budget) {
        // Cache the mirror-company → canonical-company lookup: several contacts share one employer.
        Map<UUID, UUID> canonicalCompanyByCrmCompanyId = new HashMap<>();

        var linked = 0;
        for (var contact : this.contactRepository
                .findLinkable(customerId, projectId, platform, PageRequest.of(0, budget))) {
            var companyId = this.resolveEmployer(customerId, projectId, contact, canonicalCompanyByCrmCompanyId);
            var contactId = this.contactResolver.resolve(customerId, contact, companyId);
            if (contactId == null) {
                continue;
            }
            contact.setContactId(contactId);
            this.contactRepository.save(contact);
            linked++;
        }
        return linked;
    }

    /**
     * The canonical company of the contact's employer, taken from the CRM contact ↔ company links. A contact
     * associated with several companies keeps the oldest link — deterministically, so two runs agree; the
     * contacts module holds a single current company and the full history belongs to contact jobs.
     */
    private @Nullable UUID resolveEmployer(@NonNull UUID customerId,
                                           @NonNull UUID projectId,
                                           @NonNull CrmContactEntity contact,
                                           @NonNull Map<UUID, UUID> cache) {
        var links = this.contactCompanyLinkRepository
                .findAllByContactIdAndCustomerIdAndProjectIdOrderByCreatedAtAsc(
                        contact.getId(), customerId, projectId);
        if (links.isEmpty()) {
            return null;
        }

        var crmCompanyId = links.get(0).getCompanyId();
        var cached = cache.get(crmCompanyId);
        if (cached != null) {
            return cached;
        }

        var canonical = this.companyRepository
                .findByIdAndCustomerIdAndProjectIdAndDeletedAtIsNull(crmCompanyId, customerId, projectId)
                .map(company -> company.getCompanyId())
                .orElse(null);
        if (canonical != null) {
            cache.put(crmCompanyId, canonical);
        }
        return canonical;
    }
}
