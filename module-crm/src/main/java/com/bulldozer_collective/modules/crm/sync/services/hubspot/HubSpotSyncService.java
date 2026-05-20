package com.bulldozer_collective.modules.crm.sync.services.hubspot;

import com.bulldozer_collective.modules.crm.activities.data.CrmActivityEntity;
import com.bulldozer_collective.modules.crm.activities.data.CrmActivityRepository;
import com.bulldozer_collective.modules.crm.api.CrmActivityType;
import com.bulldozer_collective.modules.crm.api.CrmDealStage;
import com.bulldozer_collective.shared.IntegrationPlatform;
import com.bulldozer_collective.modules.crm.associations.data.CrmContactCompanyLinkEntity;
import com.bulldozer_collective.modules.crm.associations.data.CrmContactCompanyLinkRepository;
import com.bulldozer_collective.modules.crm.companies.data.CrmCompanyEntity;
import com.bulldozer_collective.modules.crm.companies.data.CrmCompanyRepository;
import com.bulldozer_collective.modules.crm.contacts.data.CrmContactEntity;
import com.bulldozer_collective.modules.crm.contacts.data.CrmContactRepository;
import com.bulldozer_collective.modules.crm.deals.data.CrmDealCompanyLinkEntity;
import com.bulldozer_collective.modules.crm.deals.data.CrmDealCompanyLinkRepository;
import com.bulldozer_collective.modules.crm.deals.data.CrmDealContactLinkEntity;
import com.bulldozer_collective.modules.crm.deals.data.CrmDealContactLinkRepository;
import com.bulldozer_collective.modules.crm.deals.data.CrmDealEntity;
import com.bulldozer_collective.modules.crm.deals.data.CrmDealRepository;
import com.bulldozer_collective.modules.crm.pipelines.data.CrmDealStageMappingRepository;
import com.bulldozer_collective.modules.crm.pipelines.data.CrmPipelineRepository;
import com.bulldozer_collective.modules.crm.pipelines.data.CrmPipelineStageRepository;
import com.bulldozer_collective.modules.crm.pipelines.services.CrmPipelineUpsertService;
import com.bulldozer_collective.modules.crm.sync.services.CrmPlatformSyncService;
import com.bulldozer_collective.modules.crm.sync.services.CrmSyncResult;
import com.bulldozer_collective.shared.OAuthToken;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HubSpotSyncService implements CrmPlatformSyncService {

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

    private final HubSpotClient hubSpotClient;
    private final CrmPipelineUpsertService pipelineUpsertService;
    private final CrmPipelineRepository pipelineRepository;
    private final CrmPipelineStageRepository stageRepository;
    private final CrmDealStageMappingRepository stageMappingRepository;
    private final CrmContactRepository contactRepository;
    private final CrmCompanyRepository companyRepository;
    private final CrmDealRepository dealRepository;
    private final CrmActivityRepository activityRepository;
    private final CrmDealContactLinkRepository dealContactLinkRepository;
    private final CrmDealCompanyLinkRepository dealCompanyLinkRepository;
    private final CrmContactCompanyLinkRepository contactCompanyLinkRepository;

    @Override
    @Transactional
    public void discoverPipelines(@NonNull UUID customerId,
                                  @NonNull UUID projectId,
                                  @NonNull OAuthToken token) throws Exception {
        var pipelines = this.hubSpotClient.fetchAllDealPipelines(token.token());
        for (var hubPipeline : pipelines) {
            var pipelineEntity = this.pipelineUpsertService.upsertPipeline(
                    customerId, projectId, IntegrationPlatform.INTEGRATION_PLATFORM_HUBSPOT,
                    hubPipeline.id(), hubPipeline.label()
            );
            if (hubPipeline.stages() != null) {
                for (var stage : hubPipeline.stages()) {
                    this.pipelineUpsertService.upsertStage(
                            pipelineEntity.getId(),
                            stage.id(),
                            stage.label(),
                            stage.displayOrder()
                    );
                }
            }
        }
    }

    @Override
    @Transactional
    public CrmSyncResult sync(@NonNull UUID customerId,
                              @NonNull UUID projectId,
                              @NonNull OAuthToken token) throws Exception {
        String accessToken = token.token();
        Instant now = Instant.now();

        int contactsSynced = syncContacts(customerId, projectId, accessToken, now);
        int companiesSynced = syncCompanies(customerId, projectId, accessToken, now);
        int dealsSynced = syncDeals(customerId, projectId, accessToken, now);
        int activitiesSynced = syncActivities(customerId, projectId, accessToken, now);

        syncContactCompanyAssociations(customerId, projectId, accessToken);
        syncDealContactAssociations(customerId, projectId, accessToken);
        syncDealCompanyAssociations(customerId, projectId, accessToken);

        return CrmSyncResult.builder()
                .contactsSynced(contactsSynced)
                .companiesSynced(companiesSynced)
                .dealsSynced(dealsSynced)
                .activitiesSynced(activitiesSynced)
                .build();
    }

    private int syncContacts(UUID customerId, UUID projectId, String accessToken, Instant deletedAt) {
        Set<String> seen = new HashSet<>();
        int[] count = {0};

        this.hubSpotClient.fetchAllObjects("contacts", CONTACT_PROPERTIES, accessToken, page -> {
            for (var obj : page) {
                var props = obj.properties();
                var entity = this.contactRepository
                        .findByCustomerIdAndProjectIdAndPlatformAndSourceId(customerId, projectId, IntegrationPlatform.INTEGRATION_PLATFORM_HUBSPOT, obj.id())
                        .orElseGet(() -> {
                            var e = new CrmContactEntity();
                            e.setCustomerId(customerId);
                            e.setProjectId(projectId);
                            e.setPlatform(IntegrationPlatform.INTEGRATION_PLATFORM_HUBSPOT);
                            e.setSourceId(obj.id());
                            return e;
                        });
                entity.setFirstName(props.get("firstname"));
                entity.setLastName(props.get("lastname"));
                entity.setEmail(props.get("email"));
                entity.setPhone(props.get("phone"));
                entity.setJobTitle(props.get("jobtitle"));
                entity.setLinkedInUrl(props.get("hs_linkedin_bio"));
                this.contactRepository.save(entity);
                seen.add(obj.id());
                count[0]++;
            }
        });

        if (!seen.isEmpty()) {
            this.contactRepository.reactivateSeen(customerId, projectId, IntegrationPlatform.INTEGRATION_PLATFORM_HUBSPOT, seen);
            this.contactRepository.softDeleteMissing(customerId, projectId, IntegrationPlatform.INTEGRATION_PLATFORM_HUBSPOT, seen, deletedAt);
        }
        return count[0];
    }

    private int syncCompanies(UUID customerId, UUID projectId, String accessToken, Instant deletedAt) {
        Set<String> seen = new HashSet<>();
        int[] count = {0};

        this.hubSpotClient.fetchAllObjects("companies", COMPANY_PROPERTIES, accessToken, page -> {
            for (var obj : page) {
                var props = obj.properties();
                var name = props.getOrDefault("name", obj.id());
                var entity = this.companyRepository
                        .findByCustomerIdAndProjectIdAndPlatformAndSourceId(customerId, projectId, IntegrationPlatform.INTEGRATION_PLATFORM_HUBSPOT, obj.id())
                        .orElseGet(() -> {
                            var e = new CrmCompanyEntity();
                            e.setCustomerId(customerId);
                            e.setProjectId(projectId);
                            e.setPlatform(IntegrationPlatform.INTEGRATION_PLATFORM_HUBSPOT);
                            e.setSourceId(obj.id());
                            return e;
                        });
                entity.setName(name);
                entity.setDomain(props.get("domain"));
                entity.setIndustry(props.get("industry"));
                entity.setCountry(props.get("country"));
                entity.setCity(props.get("city"));
                this.companyRepository.save(entity);
                seen.add(obj.id());
                count[0]++;
            }
        });

        if (!seen.isEmpty()) {
            this.companyRepository.reactivateSeen(customerId, projectId, IntegrationPlatform.INTEGRATION_PLATFORM_HUBSPOT, seen);
            this.companyRepository.softDeleteMissing(customerId, projectId, IntegrationPlatform.INTEGRATION_PLATFORM_HUBSPOT, seen, deletedAt);
        }
        return count[0];
    }

    private int syncDeals(UUID customerId, UUID projectId, String accessToken, Instant deletedAt) {
        Set<String> seen = new HashSet<>();
        int[] count = {0};

        var pipelines = this.pipelineRepository.findAllByCustomerIdAndProjectIdAndPlatformAndDeletedAtIsNull(
                customerId, projectId, IntegrationPlatform.INTEGRATION_PLATFORM_HUBSPOT
        );
        var pipelineBySourceId = new java.util.HashMap<String, UUID>();
        var stageBySourceId = new java.util.HashMap<String, UUID>();
        var stageMappingByStageId = new java.util.HashMap<UUID, CrmDealStage>();

        for (var pipeline : pipelines) {
            pipelineBySourceId.put(pipeline.getSourceId(), pipeline.getId());
            for (var stage : this.stageRepository.findAllByPipelineId(pipeline.getId())) {
                stageBySourceId.put(stage.getSourceId(), stage.getId());
                this.stageMappingRepository.findByPipelineStageId(stage.getId())
                        .ifPresent(m -> stageMappingByStageId.put(stage.getId(), m.getInternalStage()));
            }
        }

        this.hubSpotClient.fetchAllObjects("deals", DEAL_PROPERTIES, accessToken, page -> {
            for (var obj : page) {
                var props = obj.properties();
                var stageSourceId = props.get("dealstage");
                var pipelineSourceId = props.get("pipeline");

                if (stageSourceId == null || pipelineSourceId == null) {
                    continue;
                }

                var pipelineId = pipelineBySourceId.get(pipelineSourceId);
                var stageId = stageBySourceId.get(stageSourceId);
                if (pipelineId == null || stageId == null) {
                    continue;
                }

                var internalStage = stageMappingByStageId.get(stageId);
                if (internalStage == null) {
                    continue;
                }

                var entity = this.dealRepository
                        .findByCustomerIdAndProjectIdAndPlatformAndSourceId(customerId, projectId, IntegrationPlatform.INTEGRATION_PLATFORM_HUBSPOT, obj.id())
                        .orElseGet(() -> {
                            var e = new CrmDealEntity();
                            e.setCustomerId(customerId);
                            e.setProjectId(projectId);
                            e.setPlatform(IntegrationPlatform.INTEGRATION_PLATFORM_HUBSPOT);
                            e.setSourceId(obj.id());
                            return e;
                        });
                entity.setName(props.getOrDefault("dealname", obj.id()));
                entity.setAmount(parseAmount(props.get("amount")));
                entity.setCurrency(props.get("deal_currency_code"));
                entity.setCloseDate(parseDate(props.get("closedate")));
                entity.setInternalStage(internalStage);
                entity.setPipelineId(pipelineId);
                entity.setPipelineStageId(stageId);
                this.dealRepository.save(entity);
                seen.add(obj.id());
                count[0]++;
            }
        });

        if (!seen.isEmpty()) {
            this.dealRepository.reactivateSeen(customerId, projectId, IntegrationPlatform.INTEGRATION_PLATFORM_HUBSPOT, seen);
            this.dealRepository.softDeleteMissing(customerId, projectId, IntegrationPlatform.INTEGRATION_PLATFORM_HUBSPOT, seen, deletedAt);
        }
        return count[0];
    }

    private int syncActivities(UUID customerId, UUID projectId, String accessToken, Instant deletedAt) {
        Set<String> seen = new HashSet<>();
        int[] count = {0};

        Map<String, CrmActivityType> engagementTypes = Map.of(
                "calls", CrmActivityType.CALL,
                "emails", CrmActivityType.EMAIL,
                "meetings", CrmActivityType.MEETING,
                "notes", CrmActivityType.NOTE,
                "tasks", CrmActivityType.TASK
        );

        for (var entry : engagementTypes.entrySet()) {
            this.hubSpotClient.fetchAllObjects(entry.getKey(), ENGAGEMENT_PROPERTIES, accessToken, page -> {
                for (var obj : page) {
                    var props = obj.properties();
                    var entity = this.activityRepository
                            .findByCustomerIdAndProjectIdAndPlatformAndSourceId(customerId, projectId, IntegrationPlatform.INTEGRATION_PLATFORM_HUBSPOT, obj.id())
                            .orElseGet(() -> {
                                var e = new CrmActivityEntity();
                                e.setCustomerId(customerId);
                                e.setProjectId(projectId);
                                e.setPlatform(IntegrationPlatform.INTEGRATION_PLATFORM_HUBSPOT);
                                e.setSourceId(obj.id());
                                return e;
                            });
                    entity.setType(entry.getValue());
                    entity.setTitle(resolveTitle(entry.getValue(), props));
                    entity.setBody(resolveBody(entry.getValue(), props));
                    entity.setDueDate(parseInstant(props.get("hs_timestamp")));
                    entity.setCompletedAt(parseInstant(props.get("hs_task_completion_date")));
                    this.activityRepository.save(entity);
                    seen.add(obj.id());
                    count[0]++;
                }
            });
        }

        if (!seen.isEmpty()) {
            this.activityRepository.reactivateSeen(customerId, projectId, IntegrationPlatform.INTEGRATION_PLATFORM_HUBSPOT, seen);
            this.activityRepository.softDeleteMissing(customerId, projectId, IntegrationPlatform.INTEGRATION_PLATFORM_HUBSPOT, seen, deletedAt);
        }
        return count[0];
    }

    private void syncContactCompanyAssociations(UUID customerId, UUID projectId, String accessToken) {
        var contacts = this.contactRepository.findAllByCustomerIdAndProjectIdAndPlatformAndDeletedAtIsNull(
                customerId, projectId, IntegrationPlatform.INTEGRATION_PLATFORM_HUBSPOT);
        if (contacts.isEmpty()) return;

        var sourceIdToLocalId = new java.util.HashMap<String, UUID>();
        contacts.forEach(c -> sourceIdToLocalId.put(c.getSourceId(), c.getId()));

        var assocMap = this.hubSpotClient.batchGetAssociations(
                "contacts", "companies", new ArrayList<>(sourceIdToLocalId.keySet()), accessToken);

        var localContactIds = contacts.stream().map(CrmContactEntity::getId).toList();
        this.contactCompanyLinkRepository.deleteAllByContactIdIn(localContactIds);

        for (var entry : assocMap.entrySet()) {
            var contactId = sourceIdToLocalId.get(entry.getKey());
            if (contactId == null) continue;

            for (var companySourceId : entry.getValue()) {
                var companyOpt = this.companyRepository.findByCustomerIdAndProjectIdAndPlatformAndSourceId(
                        customerId, projectId, IntegrationPlatform.INTEGRATION_PLATFORM_HUBSPOT, companySourceId);
                if (companyOpt.isEmpty()) continue;

                var link = new CrmContactCompanyLinkEntity();
                link.setContactId(contactId);
                link.setCompanyId(companyOpt.get().getId());
                link.setCustomerId(customerId);
                link.setProjectId(projectId);
                this.contactCompanyLinkRepository.save(link);
            }
        }
    }

    private void syncDealContactAssociations(UUID customerId, UUID projectId, String accessToken) {
        var deals = this.dealRepository.findAllByCustomerIdAndProjectIdAndPlatformAndDeletedAtIsNull(
                customerId, projectId, IntegrationPlatform.INTEGRATION_PLATFORM_HUBSPOT);
        if (deals.isEmpty()) return;

        var sourceIdToLocalId = new java.util.HashMap<String, UUID>();
        deals.forEach(d -> sourceIdToLocalId.put(d.getSourceId(), d.getId()));

        var assocMap = this.hubSpotClient.batchGetAssociations(
                "deals", "contacts", new ArrayList<>(sourceIdToLocalId.keySet()), accessToken);

        var localDealIds = deals.stream().map(CrmDealEntity::getId).toList();
        this.dealContactLinkRepository.deleteAllByDealIdIn(localDealIds);

        for (var entry : assocMap.entrySet()) {
            var dealId = sourceIdToLocalId.get(entry.getKey());
            if (dealId == null) continue;

            for (var contactSourceId : entry.getValue()) {
                var contactOpt = this.contactRepository.findByCustomerIdAndProjectIdAndPlatformAndSourceId(
                        customerId, projectId, IntegrationPlatform.INTEGRATION_PLATFORM_HUBSPOT, contactSourceId);
                if (contactOpt.isEmpty()) continue;

                var link = new CrmDealContactLinkEntity();
                link.setDealId(dealId);
                link.setContactId(contactOpt.get().getId());
                link.setCustomerId(customerId);
                link.setProjectId(projectId);
                this.dealContactLinkRepository.save(link);
            }
        }
    }

    private void syncDealCompanyAssociations(UUID customerId, UUID projectId, String accessToken) {
        var deals = this.dealRepository.findAllByCustomerIdAndProjectIdAndPlatformAndDeletedAtIsNull(
                customerId, projectId, IntegrationPlatform.INTEGRATION_PLATFORM_HUBSPOT);
        if (deals.isEmpty()) return;

        var sourceIdToLocalId = new java.util.HashMap<String, UUID>();
        deals.forEach(d -> sourceIdToLocalId.put(d.getSourceId(), d.getId()));

        var assocMap = this.hubSpotClient.batchGetAssociations(
                "deals", "companies", new ArrayList<>(sourceIdToLocalId.keySet()), accessToken);

        var localDealIds = deals.stream().map(CrmDealEntity::getId).toList();
        this.dealCompanyLinkRepository.deleteAllByDealIdIn(localDealIds);

        for (var entry : assocMap.entrySet()) {
            var dealId = sourceIdToLocalId.get(entry.getKey());
            if (dealId == null) continue;

            for (var companySourceId : entry.getValue()) {
                var companyOpt = this.companyRepository.findByCustomerIdAndProjectIdAndPlatformAndSourceId(
                        customerId, projectId, IntegrationPlatform.INTEGRATION_PLATFORM_HUBSPOT, companySourceId);
                if (companyOpt.isEmpty()) continue;

                var link = new CrmDealCompanyLinkEntity();
                link.setDealId(dealId);
                link.setCompanyId(companyOpt.get().getId());
                link.setCustomerId(customerId);
                link.setProjectId(projectId);
                this.dealCompanyLinkRepository.save(link);
            }
        }
    }

    @Nullable
    private BigDecimal parseAmount(@Nullable String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Nullable
    private Instant parseDate(@Nullable String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDate.parse(value.substring(0, 10)).atStartOfDay().toInstant(ZoneOffset.UTC);
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    private Instant parseInstant(@Nullable String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Instant.parse(value);
        } catch (Exception e) {
            return parseDate(value);
        }
    }

    @Nullable
    private String resolveTitle(CrmActivityType type, Map<String, String> props) {
        return switch (type) {
            case CALL -> props.get("hs_call_title");
            case EMAIL -> props.get("hs_email_subject");
            case MEETING -> props.get("hs_meeting_title");
            case NOTE -> null;
            case TASK -> props.get("hs_task_subject");
        };
    }

    @Nullable
    private String resolveBody(CrmActivityType type, Map<String, String> props) {
        return switch (type) {
            case CALL -> props.get("hs_call_body");
            case EMAIL -> props.get("hs_email_text");
            case MEETING -> props.get("hs_meeting_notes");
            case NOTE -> props.get("hs_note_body");
            case TASK -> props.get("hs_task_body");
        };
    }
}
