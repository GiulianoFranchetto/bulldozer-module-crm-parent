package com.bulldozer_collective.modules.crm.activities.services;

import com.bulldozer_collective.modules.crm.activities.data.CrmActivityEntity;
import com.bulldozer_collective.modules.crm.activities.data.CrmActivityRepository;
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
 * The only writer of {@code crm_activities} on the sync path. Platform adapters fetch and map; this owns the
 * transaction boundaries, which is why it can be short-lived and never spans a remote call.
 */
@Service
@RequiredArgsConstructor
public class CrmActivityPersister {

    private final CrmActivityRepository repository;

    /**
     * Upserts one CRM page. Existing rows are loaded in a single query keyed on the page's source ids — one
     * query per page rather than one per row.
     *
     * @return the number of rows written
     */
    @Transactional
    public int persistPage(@NonNull CrmSyncPass pass, @NonNull List<CrmActivityUpsert> page) {
        if (page.isEmpty()) {
            return 0;
        }

        var sourceIds = page.stream().map(CrmActivityUpsert::sourceId).toList();
        var existing = this.repository
                .findAllByCustomerIdAndProjectIdAndPlatformAndSourceIdIn(
                        pass.customerId(), pass.projectId(), pass.platform(), sourceIds)
                .stream()
                .collect(Collectors.toMap(CrmActivityEntity::getSourceId, Function.identity()));

        var batch = new ArrayList<CrmActivityEntity>(page.size());
        for (var upsert : page) {
            var entity = existing.get(upsert.sourceId());
            if (entity == null) {
                entity = new CrmActivityEntity();
                entity.setCustomerId(pass.customerId());
                entity.setProjectId(pass.projectId());
                entity.setPlatform(pass.platform());
                entity.setSourceId(upsert.sourceId());
            }
            entity.setType(upsert.type());
            entity.setTitle(upsert.title());
            entity.setBody(upsert.body());
            entity.setDueDate(upsert.dueDate());
            entity.setCompletedAt(upsert.completedAt());
            entity.setDealId(upsert.dealId());
            entity.setContactId(upsert.contactId());
            entity.setCompanyId(upsert.companyId());
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
