package com.bulldozer_collective.modules.crm.sync.services.hubspot;

import com.bulldozer_collective.modules.crm.activities.services.CrmActivityPersister;
import com.bulldozer_collective.modules.crm.activities.services.CrmActivityUpsert;
import com.bulldozer_collective.modules.crm.api.CrmActivityType;
import com.bulldozer_collective.modules.crm.api.CrmDealStage;
import com.bulldozer_collective.modules.crm.associations.services.CrmAssociationPersister;
import com.bulldozer_collective.modules.crm.companies.data.CrmCompanyEntity;
import com.bulldozer_collective.modules.crm.companies.data.CrmCompanyRepository;
import com.bulldozer_collective.modules.crm.companies.services.CrmCompanyPersister;
import com.bulldozer_collective.modules.crm.companies.services.CrmCompanyUpsert;
import com.bulldozer_collective.modules.crm.contacts.data.CrmContactEntity;
import com.bulldozer_collective.modules.crm.contacts.data.CrmContactRepository;
import com.bulldozer_collective.modules.crm.contacts.services.CrmContactPersister;
import com.bulldozer_collective.modules.crm.contacts.services.CrmContactUpsert;
import com.bulldozer_collective.modules.crm.deals.data.CrmDealEntity;
import com.bulldozer_collective.modules.crm.deals.data.CrmDealRepository;
import com.bulldozer_collective.modules.crm.deals.services.CrmDealPersister;
import com.bulldozer_collective.modules.crm.deals.services.CrmDealUpsert;
import com.bulldozer_collective.modules.crm.pipelines.data.CrmDealStageMappingRepository;
import com.bulldozer_collective.modules.crm.pipelines.data.CrmPipelineRepository;
import com.bulldozer_collective.modules.crm.pipelines.data.CrmPipelineStageRepository;
import com.bulldozer_collective.modules.crm.associations.services.ReplaceCrmLinksBuilder;
import com.bulldozer_collective.modules.crm.pipelines.services.CrmPipelineUpsertService;
import com.bulldozer_collective.modules.crm.pipelines.services.UpsertCrmPipelineBuilder;
import com.bulldozer_collective.modules.crm.pipelines.services.UpsertCrmPipelineStageBuilder;
import com.bulldozer_collective.modules.crm.shared.CrmSyncPass;
import com.bulldozer_collective.modules.crm.sync.services.CrmPlatformSyncService;
import com.bulldozer_collective.modules.crm.sync.services.CrmSyncResult;
import com.bulldozer_collective.shared.IntegrationPlatform;
import com.bulldozer_collective.shared.OAuthToken;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * HubSpot adapter: fetches, maps HubSpot's payloads onto the module's neutral upsert records, and hands them to
 * the persisters. It holds no transaction — every method here performs remote HTTP calls, and the writes belong
 * to the {@code *Persister} beans.
 */
@Service
@RequiredArgsConstructor
public class HubSpotSyncService implements CrmPlatformSyncService {

    private static final IntegrationPlatform PLATFORM = IntegrationPlatform.INTEGRATION_PLATFORM_HUBSPOT;

    private static final List<String> CONTACT_PROPERTIES = List.of(
            "firstname", "lastname", "email", "phone", "jobtitle", "hs_linkedin_bio"
    );
    private static final List<String> COMPANY_PROPERTIES = List.of(
            "name", "domain", "industry", "country", "city"
    );
    private static final List<String> DEAL_PROPERTIES = List.of(
            "dealname", "amount", "deal_currency_code", "closedate", "dealstage", "pipeline"
    );
    private static final List<String> ENGAGEMENT_PROPERTIES = List.of(
            "hs_activity_type", "hs_timestamp", "hubspot_owner_id",
            "hs_call_title", "hs_call_body",
            "hs_email_subject", "hs_email_text",
            "hs_meeting_title", "hs_meeting_notes",
            "hs_note_body",
            "hs_task_subject", "hs_task_body", "hs_task_completion_date", "hs_task_is_completed"
    );

    /** HubSpot's engagement object types mapped onto ours. Ordered, so two runs walk them the same way. */
    private static final Map<String, CrmActivityType> ENGAGEMENT_TYPES = new LinkedHashMap<>(Map.of(
            "calls", CrmActivityType.CRM_ACTIVITY_TYPE_CALL,
            "emails", CrmActivityType.CRM_ACTIVITY_TYPE_EMAIL,
            "meetings", CrmActivityType.CRM_ACTIVITY_TYPE_MEETING,
            "notes", CrmActivityType.CRM_ACTIVITY_TYPE_NOTE,
            "tasks", CrmActivityType.CRM_ACTIVITY_TYPE_TASK
    ));

    private final HubSpotClient hubSpotClient;
    private final CrmPipelineUpsertService pipelineUpsertService;
    private final CrmPipelineRepository pipelineRepository;
    private final CrmPipelineStageRepository stageRepository;
    private final CrmDealStageMappingRepository stageMappingRepository;
    private final CrmContactRepository contactRepository;
    private final CrmCompanyRepository companyRepository;
    private final CrmDealRepository dealRepository;
    private final CrmContactPersister contactPersister;
    private final CrmCompanyPersister companyPersister;
    private final CrmDealPersister dealPersister;
    private final CrmActivityPersister activityPersister;
    private final CrmAssociationPersister associationPersister;

    @Override
    public void discoverPipelines(@NonNull UUID customerId,
                                  @NonNull UUID projectId,
                                  @NonNull OAuthToken token) {
        var pipelines = this.hubSpotClient.fetchAllDealPipelines(token.token());
        for (var hubPipeline : pipelines) {
            var pipelineEntity = this.pipelineUpsertService.upsertPipeline(UpsertCrmPipelineBuilder.builder()
                    .customerId(customerId)
                    .projectId(projectId)
                    .platform(PLATFORM)
                    .sourceId(hubPipeline.id())
                    .name(hubPipeline.label())
                    .build());

            if (hubPipeline.stages() == null) {
                continue;
            }

            var seenStages = new HashSet<String>();
            for (var stage : hubPipeline.stages()) {
                this.pipelineUpsertService.upsertStage(UpsertCrmPipelineStageBuilder.builder()
                        .customerId(customerId)
                        .projectId(projectId)
                        .pipelineId(pipelineEntity.getId())
                        .sourceId(stage.id())
                        .label(stage.label())
                        .displayOrder(stage.displayOrder())
                        .build());
                seenStages.add(stage.id());
            }
            // A stage dropped in the CRM must stop being demanded by CrmSyncValidator, or every later sync
            // fails on a mapping the user can no longer create.
            this.pipelineUpsertService.pruneStages(pipelineEntity.getId(), seenStages);
        }
    }

    @Override
    public CrmSyncResult syncObjects(@NonNull UUID customerId,
                                     @NonNull UUID projectId,
                                     @NonNull OAuthToken token) {
        var accessToken = token.token();
        // One pass for the whole sweep: its startedAt is stamped on every row upserted below, and closing the
        // pass soft-deletes whatever still carries an older stamp.
        var pass = CrmSyncPass.builder()
                .customerId(customerId)
                .projectId(projectId)
                .platform(PLATFORM)
                .startedAt(Instant.now())
                .build();

        return CrmSyncResult.builder()
                .contactsSynced(this.syncContacts(pass, accessToken))
                .companiesSynced(this.syncCompanies(pass, accessToken))
                .dealsSynced(this.syncDeals(pass, accessToken))
                .activitiesSynced(this.syncActivities(pass, accessToken))
                .build();
    }

    @Override
    public void syncAssociations(@NonNull UUID customerId,
                                 @NonNull UUID projectId,
                                 @NonNull OAuthToken token) {
        var accessToken = token.token();
        this.syncContactCompanyAssociations(customerId, projectId, accessToken);
        this.syncDealContactAssociations(customerId, projectId, accessToken);
        this.syncDealCompanyAssociations(customerId, projectId, accessToken);
    }

    // ── Objects ──────────────────────────────────────────────────────────────

    private int syncContacts(CrmSyncPass pass, String accessToken) {
        var count = new int[]{0};
        this.hubSpotClient.fetchAllObjects("contacts", CONTACT_PROPERTIES, accessToken, page -> {
            var upserts = page.stream()
                    .map(obj -> CrmContactUpsert.builder()
                            .sourceId(obj.id())
                            .firstName(obj.properties().get("firstname"))
                            .lastName(obj.properties().get("lastname"))
                            .email(obj.properties().get("email"))
                            .phone(obj.properties().get("phone"))
                            .jobTitle(obj.properties().get("jobtitle"))
                            .linkedInUrl(obj.properties().get("hs_linkedin_bio"))
                            .build())
                    .toList();
            count[0] += this.contactPersister.persistPage(pass, upserts);
        });
        this.contactPersister.closePass(pass);
        return count[0];
    }

    private int syncCompanies(CrmSyncPass pass, String accessToken) {
        var count = new int[]{0};
        this.hubSpotClient.fetchAllObjects("companies", COMPANY_PROPERTIES, accessToken, page -> {
            var upserts = page.stream()
                    .map(obj -> CrmCompanyUpsert.builder()
                            .sourceId(obj.id())
                            // HubSpot allows a nameless company; the mirror column is not nullable.
                            .name(obj.properties().getOrDefault("name", obj.id()))
                            .domain(obj.properties().get("domain"))
                            .industry(obj.properties().get("industry"))
                            .country(obj.properties().get("country"))
                            .city(obj.properties().get("city"))
                            .build())
                    .toList();
            count[0] += this.companyPersister.persistPage(pass, upserts);
        });
        this.companyPersister.closePass(pass);
        return count[0];
    }

    private int syncDeals(CrmSyncPass pass, String accessToken) {
        var pipelineBySourceId = new HashMap<String, UUID>();
        var stageBySourceId = new HashMap<String, UUID>();
        var internalStageByStageId = new HashMap<UUID, CrmDealStage>();

        for (var pipeline : this.pipelineRepository
                .findAllByCustomerIdAndProjectIdAndPlatformAndDeletedAtIsNull(
                        pass.customerId(), pass.projectId(), PLATFORM)) {
            pipelineBySourceId.put(pipeline.getSourceId(), pipeline.getId());
            for (var stage : this.stageRepository.findAllByPipelineIdAndDeletedAtIsNull(pipeline.getId())) {
                stageBySourceId.put(stage.getSourceId(), stage.getId());
                this.stageMappingRepository
                        .findByPipelineStageIdAndCustomerIdAndProjectId(
                                stage.getId(), pass.customerId(), pass.projectId())
                        .ifPresent(mapping -> internalStageByStageId.put(stage.getId(), mapping.getInternalStage()));
            }
        }

        var count = new int[]{0};
        this.hubSpotClient.fetchAllObjects("deals", DEAL_PROPERTIES, accessToken, page -> {
            var upserts = new ArrayList<CrmDealUpsert>(page.size());
            for (var obj : page) {
                var props = obj.properties();
                var pipelineId = pipelineBySourceId.get(props.get("pipeline"));
                var stageId = stageBySourceId.get(props.get("dealstage"));
                if (pipelineId == null || stageId == null) {
                    // A pipeline or stage created after this sync's discovery step; the next run picks it up.
                    continue;
                }
                var internalStage = internalStageByStageId.get(stageId);
                if (internalStage == null) {
                    // Validation runs in an earlier task, so this only happens if the mapping was deleted in
                    // between. Skipping keeps internalStage non-null, which the read model relies on.
                    continue;
                }

                upserts.add(CrmDealUpsert.builder()
                        .sourceId(obj.id())
                        .name(props.getOrDefault("dealname", obj.id()))
                        .amount(parseAmount(props.get("amount")))
                        .currency(props.get("deal_currency_code"))
                        .closeDate(parseDate(props.get("closedate")))
                        .internalStage(internalStage)
                        .pipelineId(pipelineId)
                        .pipelineStageId(stageId)
                        .build());
            }
            count[0] += this.dealPersister.persistPage(pass, upserts);
        });
        this.dealPersister.closePass(pass);
        return count[0];
    }

    private int syncActivities(CrmSyncPass pass, String accessToken) {
        var count = new int[]{0};
        for (var entry : ENGAGEMENT_TYPES.entrySet()) {
            var type = entry.getValue();
            this.hubSpotClient.fetchAllObjects(entry.getKey(), ENGAGEMENT_PROPERTIES, accessToken, page -> {
                var upserts = page.stream()
                        .map(obj -> CrmActivityUpsert.builder()
                                .sourceId(obj.id())
                                .type(type)
                                .title(resolveTitle(type, obj.properties()))
                                .body(resolveBody(type, obj.properties()))
                                .dueDate(parseInstant(obj.properties().get("hs_timestamp")))
                                .completedAt(parseInstant(obj.properties().get("hs_task_completion_date")))
                                .build())
                        .toList();
                count[0] += this.activityPersister.persistPage(pass, upserts);
            });
        }
        // Closed once for all five engagement types: they share crm_activities, so closing per type would
        // soft-delete the types already synced in this same pass.
        this.activityPersister.closePass(pass);
        return count[0];
    }

    // ── Associations ─────────────────────────────────────────────────────────

    private void syncContactCompanyAssociations(UUID customerId, UUID projectId, String accessToken) {
        var contacts = this.contactRepository
                .findAllByCustomerIdAndProjectIdAndPlatformAndDeletedAtIsNull(customerId, projectId, PLATFORM);

        this.forEachBatch(contacts, CrmContactEntity::getId, CrmContactEntity::getSourceId, batch -> {
            var associations = this.hubSpotClient
                    .batchGetAssociations("contacts", "companies", batch.sourceIds(), accessToken);
            this.associationPersister.replaceContactCompanyLinks(ReplaceCrmLinksBuilder.builder()
                    .customerId(customerId)
                    .projectId(projectId)
                    .ownerIds(batch.localIds())
                    .targetsByOwner(resolve(batch,
                            this.companyIdsBySourceId(customerId, projectId, associations), associations))
                    .build());
        });
    }

    private void syncDealContactAssociations(UUID customerId, UUID projectId, String accessToken) {
        var deals = this.dealRepository
                .findAllByCustomerIdAndProjectIdAndPlatformAndDeletedAtIsNull(customerId, projectId, PLATFORM);

        this.forEachBatch(deals, CrmDealEntity::getId, CrmDealEntity::getSourceId, batch -> {
            var associations = this.hubSpotClient
                    .batchGetAssociations("deals", "contacts", batch.sourceIds(), accessToken);
            this.associationPersister.replaceDealContactLinks(ReplaceCrmLinksBuilder.builder()
                    .customerId(customerId)
                    .projectId(projectId)
                    .ownerIds(batch.localIds())
                    .targetsByOwner(resolve(batch,
                            this.contactIdsBySourceId(customerId, projectId, associations), associations))
                    .build());
        });
    }

    private void syncDealCompanyAssociations(UUID customerId, UUID projectId, String accessToken) {
        var deals = this.dealRepository
                .findAllByCustomerIdAndProjectIdAndPlatformAndDeletedAtIsNull(customerId, projectId, PLATFORM);

        this.forEachBatch(deals, CrmDealEntity::getId, CrmDealEntity::getSourceId, batch -> {
            var associations = this.hubSpotClient
                    .batchGetAssociations("deals", "companies", batch.sourceIds(), accessToken);
            this.associationPersister.replaceDealCompanyLinks(ReplaceCrmLinksBuilder.builder()
                    .customerId(customerId)
                    .projectId(projectId)
                    .ownerIds(batch.localIds())
                    .targetsByOwner(resolve(batch,
                            this.companyIdsBySourceId(customerId, projectId, associations), associations))
                    .build());
        });
    }

    /** One bounded slice of association owners: their Bulldozer ids, their CRM ids, and the mapping. */
    private record OwnerBatch(List<UUID> localIds,
                              List<String> sourceIds,
                              Map<String, UUID> localIdBySourceId) {
    }

    /**
     * Walks the owners in slices of {@link CrmAssociationPersister#MAX_OWNERS_PER_BATCH}. Not an optimisation:
     * the persister deletes by an {@code IN} list of owner ids, and a portal holds more owners than PostgreSQL
     * accepts bind parameters for. It also bounds the blast radius of a failed batch.
     */
    private <T> void forEachBatch(@NonNull List<T> owners,
                                  @NonNull Function<T, UUID> idOf,
                                  @NonNull Function<T, String> sourceIdOf,
                                  @NonNull Consumer<OwnerBatch> handler) {
        for (int from = 0; from < owners.size(); from += CrmAssociationPersister.MAX_OWNERS_PER_BATCH) {
            var slice = owners.subList(from,
                    Math.min(from + CrmAssociationPersister.MAX_OWNERS_PER_BATCH, owners.size()));
            handler.accept(new OwnerBatch(
                    slice.stream().map(idOf).toList(),
                    slice.stream().map(sourceIdOf).toList(),
                    slice.stream().collect(Collectors.toMap(sourceIdOf, idOf, (first, second) -> first))));
        }
    }

    /** Turns HubSpot's {@code sourceId → [sourceId]} associations into {@code localId → [localId]}. */
    private static Map<UUID, List<UUID>> resolve(@NonNull OwnerBatch batch,
                                                 @NonNull Map<String, UUID> targetIdBySourceId,
                                                 @NonNull Map<String, List<String>> associations) {
        var resolved = new HashMap<UUID, List<UUID>>();
        for (var entry : associations.entrySet()) {
            var ownerId = batch.localIdBySourceId().get(entry.getKey());
            if (ownerId == null) {
                continue;
            }
            var targets = entry.getValue().stream()
                    .map(targetIdBySourceId::get)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            if (!targets.isEmpty()) {
                resolved.put(ownerId, targets);
            }
        }
        return resolved;
    }

    private Map<String, UUID> companyIdsBySourceId(UUID customerId,
                                                   UUID projectId,
                                                   Map<String, List<String>> associations) {
        var sourceIds = targetSourceIds(associations);
        if (sourceIds.isEmpty()) {
            return Map.of();
        }
        return this.companyRepository
                .findAllByCustomerIdAndProjectIdAndPlatformAndSourceIdIn(customerId, projectId, PLATFORM, sourceIds)
                .stream()
                .collect(Collectors.toMap(CrmCompanyEntity::getSourceId, CrmCompanyEntity::getId));
    }

    private Map<String, UUID> contactIdsBySourceId(UUID customerId,
                                                   UUID projectId,
                                                   Map<String, List<String>> associations) {
        var sourceIds = targetSourceIds(associations);
        if (sourceIds.isEmpty()) {
            return Map.of();
        }
        return this.contactRepository
                .findAllByCustomerIdAndProjectIdAndPlatformAndSourceIdIn(customerId, projectId, PLATFORM, sourceIds)
                .stream()
                .collect(Collectors.toMap(CrmContactEntity::getSourceId, CrmContactEntity::getId));
    }

    /**
     * The distinct target ids of one batch's associations — one lookup for the whole batch instead of one per
     * association, which is what made this loop issue thousands of queries.
     */
    private static List<String> targetSourceIds(Map<String, List<String>> associations) {
        return associations.values().stream().flatMap(List::stream).distinct().toList();
    }

    // ── Mapping helpers ──────────────────────────────────────────────────────

    private static @Nullable BigDecimal parseAmount(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static @Nullable Instant parseDate(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.substring(0, 10)).atStartOfDay().toInstant(ZoneOffset.UTC);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static @Nullable Instant parseInstant(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (RuntimeException e) {
            return parseDate(value);
        }
    }

    private static @Nullable String resolveTitle(CrmActivityType type, Map<String, String> props) {
        return switch (type) {
            case CRM_ACTIVITY_TYPE_CALL -> props.get("hs_call_title");
            case CRM_ACTIVITY_TYPE_EMAIL -> props.get("hs_email_subject");
            case CRM_ACTIVITY_TYPE_MEETING -> props.get("hs_meeting_title");
            case CRM_ACTIVITY_TYPE_NOTE -> null;
            case CRM_ACTIVITY_TYPE_TASK -> props.get("hs_task_subject");
        };
    }

    private static @Nullable String resolveBody(CrmActivityType type, Map<String, String> props) {
        return switch (type) {
            case CRM_ACTIVITY_TYPE_CALL -> props.get("hs_call_body");
            case CRM_ACTIVITY_TYPE_EMAIL -> props.get("hs_email_text");
            case CRM_ACTIVITY_TYPE_MEETING -> props.get("hs_meeting_notes");
            case CRM_ACTIVITY_TYPE_NOTE -> props.get("hs_note_body");
            case CRM_ACTIVITY_TYPE_TASK -> props.get("hs_task_body");
        };
    }
}
