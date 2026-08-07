package com.bulldozer_collective.modules.crm.companies.services;

import com.bulldozer_collective.modules.crm.companies.data.CrmCompanyEntity;
import com.bulldozer_collective.modules.crm.companies.data.CrmCompanyRepository;
import com.bulldozer_collective.modules.crm.shared.CrmSyncPass;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The only writer of {@code crm_companies} on the sync path. Platform adapters fetch and map; this owns the
 * transaction boundaries, which is why it can be short-lived and never spans a remote call.
 */
@Service
@RequiredArgsConstructor
public class CrmCompanyPersister {

    private final CrmCompanyRepository repository;

    /**
     * Upserts one CRM page. Existing rows are loaded in a single query keyed on the page's source ids — one
     * query per page rather than one per row.
     *
     * @return the number of rows written
     */
    @Transactional
    public int persistPage(@NonNull CrmSyncPass pass, @NonNull List<CrmCompanyUpsert> page) {
        if (page.isEmpty()) {
            return 0;
        }

        var sourceIds = page.stream().map(CrmCompanyUpsert::sourceId).toList();
        var existing = this.repository
                .findAllByCustomerIdAndProjectIdAndPlatformAndSourceIdIn(
                        pass.customerId(), pass.projectId(), pass.platform(), sourceIds)
                .stream()
                .collect(Collectors.toMap(CrmCompanyEntity::getSourceId, Function.identity()));

        var batch = new ArrayList<CrmCompanyEntity>(page.size());
        for (var upsert : page) {
            var entity = existing.get(upsert.sourceId());
            if (entity == null) {
                entity = new CrmCompanyEntity();
                entity.setCustomerId(pass.customerId());
                entity.setProjectId(pass.projectId());
                entity.setPlatform(pass.platform());
                entity.setSourceId(upsert.sourceId());
            }
            entity.setName(upsert.name());
            entity.setDomain(upsert.domain());
            entity.setIndustry(upsert.industry());
            entity.setCountry(upsert.country());
            entity.setCity(upsert.city());
            entity.setLastSeenAt(pass.startedAt());
            batch.add(entity);
        }

        this.repository.saveAll(batch);
        return batch.size();
    }

    /**
     * Reconciles the mirror with the CRM once every page has been persisted: rows seen again come back, rows
     * the pass never saw are soft-deleted.
     *
     * <p>Must only be called after a <b>complete</b> successful iteration. Called after a partial one, every
     * page that was not reached yet would be soft-deleted.
     */
    @Transactional
    public void closePass(@NonNull CrmSyncPass pass) {
        this.repository.reactivateSeenSince(pass.customerId(), pass.projectId(), pass.platform(), pass.startedAt());
        this.repository.softDeleteNotSeenSince(
                pass.customerId(), pass.projectId(), pass.platform(), pass.startedAt(), Instant.now());
    }
}
