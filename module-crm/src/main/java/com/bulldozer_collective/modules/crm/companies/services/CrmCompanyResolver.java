package com.bulldozer_collective.modules.crm.companies.services;

import com.bulldozer_collective.companies.api.CreateCompanyRequest;
import com.bulldozer_collective.companies.api.SearchCompaniesRequest;
import com.bulldozer_collective.companies.api.clients.InternalCreateCompanyClient;
import com.bulldozer_collective.companies.api.clients.SearchCompaniesClient;
import com.bulldozer_collective.logging.BulldozerLogger;
import com.bulldozer_collective.modules.crm.companies.data.CrmCompanyEntity;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static com.bulldozer_collective.modules.crm.configurations.CrmLogContext.op;

/**
 * Resolves a mirrored CRM company to the canonical Bulldozer company: searches by domain first, and only
 * creates one when nothing matches. Talks to the companies module over its internal HTTP clients only —
 * never its services.
 *
 * <p>Returns {@code null} when the CRM record carries no domain: the companies module requires a website, so
 * there is nothing to create. That is a normal outcome, not an error — the mirror row simply stays unlinked.
 *
 * <p>Symmetric to {@code module-outbound}'s {@code CompanyResolver}.
 */
@Service
@RequiredArgsConstructor
public class CrmCompanyResolver {

    private final BulldozerLogger logger;

    public @Nullable UUID resolve(@NonNull UUID customerId,
                                  @NonNull UUID projectId,
                                  @NonNull CrmCompanyEntity company) {
        var domain = company.getDomain();
        if (isBlank(domain)) {
            this.logger.warn(op("CRM-004", customerId, projectId)
                    + "CRM company %s has no domain, cannot link it to a Bulldozer company",
                    company.getSourceId());
            return null;
        }

        var existing = this.searchByWebsite(customerId, domain);
        if (existing != null) {
            this.logger.debug(op("CRM-004", customerId, projectId)
                    + "Reusing existing company %s for CRM company %s", existing, company.getSourceId());
            return existing;
        }

        var response = InternalCreateCompanyClient.builder()
                .customerId(customerId)
                .request(CreateCompanyRequest.builder()
                        .name(isBlank(company.getName()) ? domain : company.getName())
                        .website(domain)
                        .projectId(projectId)
                        // Enrichment is the companies module's own concern and costs jetons; a CRM sync
                        // mirrors what the CRM says and must not trigger it for every synced account.
                        .enrichment(false)
                        .build())
                .build()
                .execute();

        if (response == null || response.company() == null) {
            this.logger.warn(op("CRM-004", customerId, projectId)
                    + "Company creation returned nothing for domain %s", domain);
            return null;
        }

        this.logger.debug(op("CRM-004", customerId, projectId) + "Created company %s for CRM company %s",
                response.company().id(), company.getSourceId());
        return response.company().id();
    }

    private @Nullable UUID searchByWebsite(@NonNull UUID customerId, @NonNull String website) {
        var response = SearchCompaniesClient.builder()
                .customerId(customerId)
                .request(new SearchCompaniesRequest(null, null, null, website))
                .build()
                .execute();
        if (response != null && response.companies() != null && !response.companies().getContent().isEmpty()) {
            return response.companies().getContent().get(0).id();
        }
        return null;
    }

    private static boolean isBlank(@Nullable String value) {
        return value == null || value.isBlank();
    }
}
