package com.bulldozer_collective.modules.crm.imports.services;

import com.bulldozer_collective.companies.api.Company;
import com.bulldozer_collective.contacts.api.Contact;
import com.bulldozer_collective.contacts.api.ContactJob;
import com.bulldozer_collective.logging.BulldozerLogger;
import com.bulldozer_collective.modules.crm.api.CrmCompany;
import com.bulldozer_collective.modules.crm.api.CrmContact;
import com.bulldozer_collective.modules.crm.api.CrmImportCounts;
import com.bulldozer_collective.modules.crm.api.CrmImportSkip;
import com.bulldozer_collective.modules.crm.api.CrmObjectRef;
import com.bulldozer_collective.modules.crm.api.CrmObjectType;
import com.bulldozer_collective.modules.crm.api.ImportCrmLeadListResponse;
import com.bulldozer_collective.modules.crm.companies.services.SaveCrmCompanyBuilder;
import com.bulldozer_collective.modules.crm.contacts.services.SaveCrmContactBuilder;
import com.bulldozer_collective.modules.crm.gateway.CrmGateway;
import com.bulldozer_collective.modules.crm.gateway.CrmGatewayResolver;
import com.bulldozer_collective.modules.crm.gateway.CrmLink;
import com.bulldozer_collective.modules.crm.gateway.CrmNote;
import com.bulldozer_collective.modules.crm.gateway.CrmSession;
import com.bulldozer_collective.modules.crm.gateway.CrmSessionOpener;
import com.bulldozer_collective.modules.crm.gateway.NoTokenException;
import com.bulldozer_collective.modules.outbound.lead_lists.Lead;
import com.bulldozer_collective.modules.outbound.lead_lists.LeadType;
import com.bulldozer_collective.shared.exceptions.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.bulldozer_collective.modules.crm.configurations.CrmLogContext.op;

/**
 * Pushes the accepted leads of a lead list into the project's CRM: each contact, and each company — a company lead's,
 * or a contact lead's own employer. A record the CRM already has is matched (contact by email, else by exact name;
 * company by domain, else by exact name) and only its empty fields are filled; a name matching several records is
 * ambiguous and skipped. Each record created, or changed — a field filled or an association added — gets a note
 * naming the list. Nothing is stored: rerunning an import finds what the previous one wrote and changes nothing.
 */
@Service
@RequiredArgsConstructor
public class CrmLeadListImporter {

    static final String BULLDOZER_URL = "https://bulldozer-os.fr";

    private final CrmLeadListSource source;
    private final CrmSessionOpener sessionOpener;
    private final CrmGatewayResolver gatewayResolver;
    private final BulldozerLogger logger;

    /**
     * @throws NotFoundException        when the project has no such lead list.
     * @throws IllegalArgumentException when the list holds too many accepted leads.
     */
    public @NonNull ImportCrmLeadListResponse importLeadList(@NonNull UUID customerId,
                                                             @NonNull UUID projectId,
                                                             @NonNull UUID connectionId,
                                                             @NonNull UUID leadListId,
                                                             @Nullable String legalBasis) throws NoTokenException {
        var listName = this.source.name(customerId, projectId, leadListId);
        var leads = this.source.acceptedLeads(customerId, projectId, leadListId);
        var session = this.sessionOpener.open(customerId, projectId, connectionId);
        var run = new Run(session, this.gatewayResolver.resolve(session), legalBasis);

        // What the leads ask for, deduplicated: two leads naming the same person or company give one record.
        var employers = this.source.companies(customerId, leads.stream()
                .filter(lead -> lead.type() == LeadType.LEAD_TYPE_CONTACT && lead.contact() != null)
                .map(lead -> lead.contact().companyId())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));
        for (var lead : leads) {
            if (lead.type() == LeadType.LEAD_TYPE_COMPANY) {
                if (lead.company() == null) {
                    run.skip(lead.id(), "The lead's company could not be read");
                } else {
                    run.wantCompany(lead.id(), lead.company());
                }
                continue;
            }
            if (lead.contact() == null) {
                run.skip(lead.id(), "The lead's contact could not be read");
                continue;
            }
            var employer = lead.contact().companyId() == null ? null : employers.get(lead.contact().companyId());
            var companyKey = employer == null ? null : run.wantCompany(lead.id(), employer);
            run.wantContact(lead.id(), lead.contact(), companyKey);
        }

        run.resolveCompanies();
        run.resolveContacts();
        run.associate();
        run.logNotes(note(listName));

        var response = new ImportCrmLeadListResponse(
                counts(run.contacts.values()), counts(run.companies.values()), run.skipped);
        this.logger.info(op("CRM-004", customerId, projectId)
                        + "Imported lead list %s on connection %s: contacts %s, companies %s, %d skipped",
                leadListId, connectionId, response.contacts(), response.companies(), response.skipped().size());
        return response;
    }

    /** The note logged on every record an import creates or changes. */
    static @NonNull String note(@NonNull String listName) {
        return "<p>From " + HtmlUtils.htmlEscape(listName) + "</p>"
                + "<p><a href=\"" + BULLDOZER_URL + "\">Imported with Bulldozer OS</a></p>";
    }

    /** A website as a CRM domain: no scheme, {@code www.}, port, path or query. */
    static @Nullable String domain(@Nullable String website) {
        if (website == null || website.isBlank()) {
            return null;
        }
        var domain = website.trim().toLowerCase(Locale.ROOT).replaceFirst("^[a-z][a-z0-9+.-]*://", "");
        for (var end : List.of("/", "?", "#", ":")) {
            var index = domain.indexOf(end);
            if (index >= 0) {
                domain = domain.substring(0, index);
            }
        }
        domain = domain.startsWith("www.") ? domain.substring(4) : domain;
        return domain.isEmpty() ? null : domain;
    }

    private static CrmImportCounts counts(@NonNull Iterable<? extends Wanted> records) {
        int created = 0, updated = 0, unchanged = 0;
        for (var record : records) {
            if (record.crmId == null) {
                continue;
            }
            if (record.created) {
                created++;
            } else if (record.changed) {
                updated++;
            } else {
                unchanged++;
            }
        }
        return new CrmImportCounts(created, updated, unchanged);
    }

    // ── one import ───────────────────────────────────────────────────────────

    /** A record an import asks for, and what became of it in the CRM. */
    private abstract static class Wanted {
        final String key;
        final Set<UUID> leadIds = new LinkedHashSet<>();
        @Nullable String crmId;
        boolean created;
        boolean changed;

        Wanted(String key) {
            this.key = key;
        }
    }

    private static final class WantedCompany extends Wanted {
        final SaveCrmCompanyBuilder fields;
        final @Nullable String domain;

        WantedCompany(String key, SaveCrmCompanyBuilder fields, @Nullable String domain) {
            super(key);
            this.fields = fields;
            this.domain = domain;
        }
    }

    private static final class WantedContact extends Wanted {
        final SaveCrmContactBuilder fields;
        final @Nullable String companyKey;

        WantedContact(String key, SaveCrmContactBuilder fields, @Nullable String companyKey) {
            super(key);
            this.fields = fields;
            this.companyKey = companyKey;
        }
    }

    private static final class Run {
        final CrmSession session;
        final CrmGateway gateway;
        final @Nullable String legalBasis;
        final Map<String, WantedCompany> companies = new LinkedHashMap<>();
        final Map<String, WantedContact> contacts = new LinkedHashMap<>();
        final List<CrmImportSkip> skipped = new ArrayList<>();

        Run(CrmSession session, CrmGateway gateway, @Nullable String legalBasis) {
            this.session = session;
            this.gateway = gateway;
            this.legalBasis = legalBasis;
        }

        void skip(@Nullable UUID leadId, String reason) {
            this.skipped.add(new CrmImportSkip(leadId, reason));
        }

        void skip(Wanted record, String reason) {
            record.leadIds.forEach(leadId -> this.skip(leadId, reason));
        }

        /** @return the company's key. */
        String wantCompany(UUID leadId, Company company) {
            var domain = domain(company.website());
            var key = domain != null ? "domain:" + domain : "name:" + company.name().trim().toLowerCase(Locale.ROOT);
            var address = company.address();
            var wanted = this.companies.computeIfAbsent(key, k -> new WantedCompany(k, SaveCrmCompanyBuilder.builder()
                    .name(company.name())
                    .domain(domain)
                    .city(address == null ? null : address.city())
                    .country(address == null ? null : address.country())
                    .build(), domain));
            wanted.leadIds.add(leadId);
            return key;
        }

        void wantContact(UUID leadId, Contact contact, @Nullable String companyKey) {
            var email = contact.email() == null || contact.email().isBlank() ? null
                    : contact.email().trim().toLowerCase(Locale.ROOT);
            var key = email != null ? "email:" + email : nameKey(contact.firstName(), contact.lastName());
            var wanted = this.contacts.computeIfAbsent(key, k -> new WantedContact(k, SaveCrmContactBuilder.builder()
                    .firstName(contact.firstName())
                    .lastName(contact.lastName())
                    .email(email)
                    .phone(contact.phone())
                    .jobTitle(currentTitle(contact))
                    .build(), companyKey));
            wanted.leadIds.add(leadId);
        }

        void resolveCompanies() {
            var byDomain = group(this.gateway.findCompaniesByDomain(this.session, this.companies.values().stream()
                    .map(company -> company.domain)
                    .filter(Objects::nonNull)
                    .toList()), company -> domain(company.domain()));
            var toCreate = new ArrayList<WantedCompany>();
            var toUpdate = new ArrayList<WantedCompany>();
            var fills = new ArrayList<SaveCrmCompanyBuilder>();
            for (var wanted : this.companies.values()) {
                var matches = wanted.domain != null
                        ? byDomain.getOrDefault(wanted.domain, List.of())
                        : this.gateway.findCompaniesByName(this.session, Objects.requireNonNull(wanted.fields.name()));
                if (matches.size() > 1) {
                    this.skip(wanted, "%d companies in the CRM match %s".formatted(matches.size(), describe(wanted)));
                } else if (matches.isEmpty()) {
                    toCreate.add(wanted);
                } else {
                    var existing = matches.get(0);
                    wanted.crmId = existing.id();
                    var fill = fill(existing, wanted.fields);
                    if (!fill.isEmpty()) {
                        toUpdate.add(wanted);
                        fills.add(fill);
                    }
                }
            }

            var created = this.gateway.createCompanies(this.session, toCreate.stream().map(w -> w.fields).toList());
            created.failures().forEach((index, reason) -> this.skip(toCreate.get(index), reason));
            var createdByKey = group(created.done(), company -> {
                var domain = domain(company.domain());
                return domain != null ? "domain:" + domain : "name:" + Objects.toString(company.name(), "")
                        .trim().toLowerCase(Locale.ROOT);
            });
            for (var wanted : toCreate) {
                var match = createdByKey.getOrDefault(wanted.key, List.of());
                if (!match.isEmpty()) {
                    wanted.crmId = match.get(0).id();
                    wanted.created = true;
                }
            }

            var updated = this.gateway.updateCompanies(this.session,
                    toUpdate.stream().map(w -> Objects.requireNonNull(w.crmId)).toList(), fills);
            for (int index = 0; index < toUpdate.size(); index++) {
                var reason = updated.failures().get(index);
                if (reason == null) {
                    toUpdate.get(index).changed = true;
                } else {
                    this.skip(toUpdate.get(index), "Not completed in the CRM: " + reason);
                }
            }
        }

        void resolveContacts() {
            var byEmail = group(this.gateway.findContactsByEmail(this.session, this.contacts.values().stream()
                            .map(contact -> contact.fields.email())
                            .filter(Objects::nonNull)
                            .toList()),
                    contact -> contact.email() == null ? null : contact.email().trim().toLowerCase(Locale.ROOT));
            var toCreate = new ArrayList<WantedContact>();
            var toUpdate = new ArrayList<WantedContact>();
            var fills = new ArrayList<SaveCrmContactBuilder>();
            for (var wanted : this.contacts.values()) {
                var email = wanted.fields.email();
                var matches = email != null
                        ? byEmail.getOrDefault(email, List.of())
                        : this.gateway.findContactsByName(this.session,
                        Objects.requireNonNull(wanted.fields.firstName()),
                        Objects.requireNonNull(wanted.fields.lastName()));
                if (matches.size() > 1) {
                    this.skip(wanted, "%d contacts in the CRM match %s".formatted(matches.size(), describe(wanted)));
                } else if (matches.isEmpty()) {
                    toCreate.add(wanted);
                } else {
                    var existing = matches.get(0);
                    wanted.crmId = existing.id();
                    var fill = fill(existing, wanted.fields);
                    if (!fill.isEmpty()) {
                        toUpdate.add(wanted);
                        fills.add(fill);
                    }
                }
            }

            var created = this.gateway.createContacts(this.session, toCreate.stream()
                    .map(w -> w.fields.toBuilder().legalBasis(this.legalBasis).build())
                    .toList());
            created.failures().forEach((index, reason) -> this.skip(toCreate.get(index), reason));
            var createdByKey = group(created.done(), contact -> contact.email() != null
                    ? "email:" + contact.email().trim().toLowerCase(Locale.ROOT)
                    : nameKey(contact.firstName(), contact.lastName()));
            for (var wanted : toCreate) {
                var match = createdByKey.getOrDefault(wanted.key, List.of());
                if (!match.isEmpty()) {
                    wanted.crmId = match.get(0).id();
                    wanted.created = true;
                }
            }

            var updated = this.gateway.updateContacts(this.session,
                    toUpdate.stream().map(w -> Objects.requireNonNull(w.crmId)).toList(), fills);
            for (int index = 0; index < toUpdate.size(); index++) {
                var reason = updated.failures().get(index);
                if (reason == null) {
                    toUpdate.get(index).changed = true;
                } else {
                    this.skip(toUpdate.get(index), "Not completed in the CRM: " + reason);
                }
            }
        }

        /** Associates each contact with its company, unless the CRM already does. */
        void associate() {
            var pairs = new ArrayList<Map.Entry<WantedContact, WantedCompany>>();
            for (var contact : this.contacts.values()) {
                var company = contact.companyKey == null ? null : this.companies.get(contact.companyKey);
                if (contact.crmId != null && company != null && company.crmId != null) {
                    pairs.add(Map.entry(contact, company));
                }
            }
            var existingIds = pairs.stream()
                    .map(Map.Entry::getKey)
                    .filter(contact -> !contact.created)
                    .map(contact -> Objects.requireNonNull(contact.crmId))
                    .collect(Collectors.toSet());
            var associated = existingIds.isEmpty() ? Map.<String, Set<String>>of()
                    : this.gateway.listAssociatedIds(this.session, CrmObjectType.CRM_OBJECT_TYPE_CONTACT, existingIds,
                    CrmObjectType.CRM_OBJECT_TYPE_COMPANY);
            var missing = pairs.stream()
                    .filter(pair -> !associated.getOrDefault(pair.getKey().crmId, Set.of())
                            .contains(pair.getValue().crmId))
                    .toList();
            if (missing.isEmpty()) {
                return;
            }
            var errors = this.gateway.associateAll(this.session, CrmObjectType.CRM_OBJECT_TYPE_CONTACT,
                    CrmObjectType.CRM_OBJECT_TYPE_COMPANY, missing.stream()
                            .map(pair -> new CrmLink(Objects.requireNonNull(pair.getKey().crmId),
                                    Objects.requireNonNull(pair.getValue().crmId)))
                            .toList());
            // The CRM does not always say which link it refused: count none as a change then.
            if (errors.isEmpty()) {
                missing.forEach(pair -> {
                    pair.getKey().changed = true;
                    pair.getValue().changed = true;
                });
            } else {
                errors.forEach(error -> this.skip((UUID) null, "Association not created: " + error));
            }
        }

        void logNotes(String body) {
            var targets = new ArrayList<Wanted>();
            var notes = new ArrayList<CrmNote>();
            var now = Instant.now();
            this.companies.values().stream().filter(c -> c.crmId != null && (c.created || c.changed)).forEach(c -> {
                targets.add(c);
                notes.add(new CrmNote(body, now, new CrmObjectRef(CrmObjectType.CRM_OBJECT_TYPE_COMPANY, c.crmId)));
            });
            this.contacts.values().stream().filter(c -> c.crmId != null && (c.created || c.changed)).forEach(c -> {
                targets.add(c);
                notes.add(new CrmNote(body, now, new CrmObjectRef(CrmObjectType.CRM_OBJECT_TYPE_CONTACT, c.crmId)));
            });
            if (notes.isEmpty()) {
                return;
            }
            this.gateway.createNotes(this.session, notes).failures()
                    .forEach((index, reason) -> this.skip(targets.get(index), "Note not logged: " + reason));
        }
    }

    // ── matching and filling ─────────────────────────────────────────────────

    /** Only the fields the CRM record leaves empty. */
    static @NonNull SaveCrmCompanyBuilder fill(@NonNull CrmCompany existing, @NonNull SaveCrmCompanyBuilder wanted) {
        return SaveCrmCompanyBuilder.builder()
                .name(blank(existing.name()) ? wanted.name() : null)
                .domain(blank(existing.domain()) ? wanted.domain() : null)
                .city(blank(existing.city()) ? wanted.city() : null)
                .country(blank(existing.country()) ? wanted.country() : null)
                .build();
    }

    /** Only the fields the CRM record leaves empty. */
    static @NonNull SaveCrmContactBuilder fill(@NonNull CrmContact existing, @NonNull SaveCrmContactBuilder wanted) {
        return SaveCrmContactBuilder.builder()
                .firstName(blank(existing.firstName()) ? wanted.firstName() : null)
                .lastName(blank(existing.lastName()) ? wanted.lastName() : null)
                .email(blank(existing.email()) ? wanted.email() : null)
                .phone(blank(existing.phone()) ? wanted.phone() : null)
                .jobTitle(blank(existing.jobTitle()) ? wanted.jobTitle() : null)
                .build();
    }

    /** The title of a position the contact still holds, if any. */
    private static @Nullable String currentTitle(@NonNull Contact contact) {
        var now = Instant.now();
        return contact.jobs().stream()
                .filter(job -> job.to() == null || job.to().isAfter(now))
                .map(ContactJob::title)
                .filter(title -> !title.isBlank())
                .findFirst()
                .orElse(null);
    }

    private static String nameKey(@Nullable String firstName, @Nullable String lastName) {
        return "name:" + Objects.toString(firstName, "").trim().toLowerCase(Locale.ROOT)
                + "|" + Objects.toString(lastName, "").trim().toLowerCase(Locale.ROOT);
    }

    private static String describe(@NonNull Wanted wanted) {
        return wanted.key.substring(wanted.key.indexOf(':') + 1);
    }

    private static boolean blank(@Nullable String value) {
        return value == null || value.isBlank();
    }

    private static <T> Map<String, List<T>> group(@NonNull List<T> records, @NonNull Function<T, @Nullable String> key) {
        var grouped = new HashMap<String, List<T>>();
        for (var record : records) {
            var value = key.apply(record);
            if (value != null) {
                grouped.computeIfAbsent(value, k -> new ArrayList<>()).add(record);
            }
        }
        return grouped;
    }
}
