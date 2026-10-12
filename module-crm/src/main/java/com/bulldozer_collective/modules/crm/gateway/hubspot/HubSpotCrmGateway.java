package com.bulldozer_collective.modules.crm.gateway.hubspot;

import com.bulldozer_collective.modules.crm.activities.services.CreateCrmActivityBuilder;
import com.bulldozer_collective.modules.crm.api.CrmActivity;
import com.bulldozer_collective.modules.crm.api.CrmActivityType;
import com.bulldozer_collective.modules.crm.api.CrmCompany;
import com.bulldozer_collective.modules.crm.api.CrmContact;
import com.bulldozer_collective.modules.crm.api.CrmContainerType;
import com.bulldozer_collective.modules.crm.api.CrmDeal;
import com.bulldozer_collective.modules.crm.api.CrmLead;
import com.bulldozer_collective.modules.crm.api.CrmObjectRef;
import com.bulldozer_collective.modules.crm.api.CrmObjectType;
import com.bulldozer_collective.modules.crm.api.CrmPage;
import com.bulldozer_collective.modules.crm.api.CrmProperty;
import com.bulldozer_collective.modules.crm.gateway.CrmRemoteContainer;
import com.bulldozer_collective.modules.crm.companies.services.SaveCrmCompanyBuilder;
import com.bulldozer_collective.modules.crm.contacts.services.SaveCrmContactBuilder;
import com.bulldozer_collective.modules.crm.deals.services.SaveCrmDealBuilder;
import com.bulldozer_collective.modules.crm.gateway.CrmContainedRecord;
import com.bulldozer_collective.modules.crm.gateway.CrmGateway;
import com.bulldozer_collective.modules.crm.gateway.CrmSession;
import com.bulldozer_collective.modules.crm.gateway.CrmStageOption;
import com.bulldozer_collective.modules.crm.gateway.CrmStageWrite;
import com.bulldozer_collective.modules.crm.gateway.CrmUpstreamException;
import com.bulldozer_collective.modules.crm.gateway.ListCrmObjectsBuilder;
import com.bulldozer_collective.modules.crm.gateway.hubspot.dto.HubSpotObject;
import com.bulldozer_collective.modules.crm.gateway.hubspot.dto.HubSpotPipeline;
import com.bulldozer_collective.modules.crm.gateway.hubspot.dto.HubSpotProperty;
import com.bulldozer_collective.modules.crm.gateway.hubspot.dto.HubSpotResults;
import com.bulldozer_collective.modules.crm.leads.services.CreateCrmLeadBuilder;
import com.bulldozer_collective.shared.IntegrationPlatform;
import com.bulldozer_collective.shared.exceptions.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static com.bulldozer_collective.modules.crm.gateway.hubspot.HubSpotClient.API_VERSION;

/**
 * HubSpot implementation of {@link CrmGateway}, on the {@value HubSpotClient#API_VERSION} CRM API.
 *
 * <p>A container is a HubSpot pipeline. A plain listing goes through {@code GET /crm/objects/{version}/{type}};
 * a listing restricted to a pipeline, a full-text query or an association filter goes through
 * {@code POST …/{type}/search}, which HubSpot indexes with a few seconds' delay and limits to five requests per
 * second and 10,000 results per query. Associations are created with HubSpot's default (unlabelled) association
 * type, which needs no association type id.
 */
@Component
@RequiredArgsConstructor
public class HubSpotCrmGateway implements CrmGateway {

    private static final TypeReference<HubSpotResults<HubSpotObject>> OBJECTS = new TypeReference<>() {};
    private static final TypeReference<HubSpotObject> OBJECT = new TypeReference<>() {};
    private static final TypeReference<HubSpotResults<HubSpotPipeline>> PIPELINES = new TypeReference<>() {};
    private static final TypeReference<HubSpotResults<HubSpotProperty>> PROPERTIES = new TypeReference<>() {};

    /**
     * The lead → contact association a lead must be created with, as given by the leads guide's create example
     * ({@code HUBSPOT_DEFINED}, type {@code 578}).
     */
    private static final int LEAD_TO_CONTACT_ASSOCIATION_TYPE = 578;

    private final HubSpotClientFactory clientFactory;

    @Override
    public @NonNull IntegrationPlatform platform() {
        return IntegrationPlatform.INTEGRATION_PLATFORM_HUBSPOT;
    }

    // ── contacts ─────────────────────────────────────────────────────────────

    @Override
    public @NonNull CrmPage<CrmContact> listContacts(@NonNull CrmSession session,
                                                     @NonNull ListCrmObjectsBuilder query) {
        return this.list(session, CrmObjectType.CRM_OBJECT_TYPE_CONTACT, query, null, HubSpotMapper.CONTACT_PROPERTIES,
                HubSpotMapper::contact);
    }

    @Override
    public @NonNull CrmContact getContact(@NonNull CrmSession session, @NonNull String id) {
        return HubSpotMapper.contact(
                this.get(session, CrmObjectType.CRM_OBJECT_TYPE_CONTACT, id, HubSpotMapper.CONTACT_PROPERTIES));
    }

    @Override
    public @NonNull String createContact(@NonNull CrmSession session, @NonNull SaveCrmContactBuilder contact) {
        return this.create(session, CrmObjectType.CRM_OBJECT_TYPE_CONTACT, HubSpotMapper.contact(contact), List.of());
    }

    @Override
    public void updateContact(@NonNull CrmSession session,
                              @NonNull String id,
                              @NonNull SaveCrmContactBuilder contact) {
        this.update(session, CrmObjectType.CRM_OBJECT_TYPE_CONTACT, id, HubSpotMapper.contact(contact));
    }

    // ── companies ────────────────────────────────────────────────────────────

    @Override
    public @NonNull CrmPage<CrmCompany> listCompanies(@NonNull CrmSession session,
                                                      @NonNull ListCrmObjectsBuilder query) {
        return this.list(session, CrmObjectType.CRM_OBJECT_TYPE_COMPANY, query, null, HubSpotMapper.COMPANY_PROPERTIES,
                HubSpotMapper::company);
    }

    @Override
    public @NonNull CrmCompany getCompany(@NonNull CrmSession session, @NonNull String id) {
        return HubSpotMapper.company(
                this.get(session, CrmObjectType.CRM_OBJECT_TYPE_COMPANY, id, HubSpotMapper.COMPANY_PROPERTIES));
    }

    @Override
    public @NonNull String createCompany(@NonNull CrmSession session, @NonNull SaveCrmCompanyBuilder company) {
        return this.create(session, CrmObjectType.CRM_OBJECT_TYPE_COMPANY, HubSpotMapper.company(company), List.of());
    }

    @Override
    public void updateCompany(@NonNull CrmSession session,
                              @NonNull String id,
                              @NonNull SaveCrmCompanyBuilder company) {
        this.update(session, CrmObjectType.CRM_OBJECT_TYPE_COMPANY, id, HubSpotMapper.company(company));
    }

    // ── containers ───────────────────────────────────────────────────────────

    @Override
    public @NonNull List<CrmRemoteContainer> listRemoteContainers(@NonNull CrmSession session,
                                                                  @NonNull CrmContainerType type) {
        var kind = HubSpotContainerKind.of(type);
        var response = this.clientFactory.create(session)
                .get(List.of("crm", "pipelines", API_VERSION, kind.pipelineTypeId()), Map.of(), PIPELINES);
        return results(response).stream()
                .filter(pipeline -> pipeline.id() != null && !Boolean.TRUE.equals(pipeline.archived()))
                .map(HubSpotMapper::pipeline)
                .sorted(Comparator.comparingInt(CrmRemoteContainer::displayOrder))
                .toList();
    }

    @Override
    public @NonNull String nativeStageProperty(@NonNull CrmContainerType type) {
        return HubSpotContainerKind.of(type).nativeStageProperty();
    }

    @Override
    public @NonNull List<CrmProperty> listProperties(@NonNull CrmSession session, @NonNull CrmContainerType type) {
        var kind = HubSpotContainerKind.of(type);
        var response = this.clientFactory.create(session)
                .get(List.of("crm", "properties", API_VERSION, kind.propertiesPath()), Map.of(), PROPERTIES);
        return results(response).stream()
                .filter(property -> property.name() != null
                        && !Boolean.TRUE.equals(property.archived())
                        && !Boolean.TRUE.equals(property.hidden()))
                .map(HubSpotMapper::property)
                .sorted(Comparator.comparing(CrmProperty::label, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    /**
     * The native stage property takes the stage ids of the record's own pipeline — its property definition lists
     * the stages of every pipeline, so it cannot be used. Any other property takes the values of its options.
     */
    @Override
    public @NonNull List<CrmStageOption> listStageOptions(@NonNull CrmSession session,
                                                          @NonNull CrmContainerType type,
                                                          @NonNull String containerRemoteId,
                                                          @NonNull String propertyName) {
        var kind = HubSpotContainerKind.of(type);
        if (kind.nativeStageProperty().equals(propertyName)) {
            return this.listRemoteContainers(session, type).stream()
                    .filter(pipeline -> pipeline.remoteId().equals(containerRemoteId))
                    .findFirst()
                    .map(pipeline -> pipeline.stages().stream()
                            .map(stage -> new CrmStageOption(stage.remoteId(), stage.name(), stage.displayOrder()))
                            .toList())
                    .orElseThrow(() -> new NotFoundException("HubSpot: no pipeline " + containerRemoteId));
        }
        return this.listProperties(session, type).stream()
                .filter(property -> property.name().equals(propertyName))
                .findFirst()
                .map(property -> property.options().stream()
                        .filter(option -> !option.hidden())
                        .map(option -> new CrmStageOption(option.value(), option.label(), option.displayOrder()))
                        .toList())
                .orElseThrow(() -> new IllegalArgumentException(
                        "propertyName: '%s' is not a property of this HubSpot account".formatted(propertyName)));
    }

    // ── deals ────────────────────────────────────────────────────────────────

    @Override
    public @NonNull CrmPage<CrmContainedRecord<CrmDeal>> listDeals(@NonNull CrmSession session,
                                                                   @NonNull String containerRemoteId,
                                                                   @NonNull ListCrmObjectsBuilder query,
                                                                   @NonNull String stageProperty) {
        return this.list(session, CrmObjectType.CRM_OBJECT_TYPE_DEAL, query,
                Map.of(HubSpotContainerKind.DEALS.pipelineProperty(), containerRemoteId),
                with(HubSpotMapper.DEAL_PROPERTIES, stageProperty), HubSpotMapper::deal);
    }

    @Override
    public @NonNull CrmContainedRecord<CrmDeal> getDeal(@NonNull CrmSession session,
                                                        @NonNull String id,
                                                        @NonNull String stageProperty) {
        return HubSpotMapper.deal(this.get(session, CrmObjectType.CRM_OBJECT_TYPE_DEAL, id,
                with(HubSpotMapper.DEAL_PROPERTIES, stageProperty)));
    }

    @Override
    public @NonNull String createDeal(@NonNull CrmSession session,
                                      @NonNull String containerRemoteId,
                                      @NonNull SaveCrmDealBuilder deal,
                                      @NonNull List<CrmStageWrite> stages) {
        return this.create(session, CrmObjectType.CRM_OBJECT_TYPE_DEAL,
                HubSpotMapper.deal(deal, containerRemoteId, stages), List.of());
    }

    @Override
    public void updateDeal(@NonNull CrmSession session,
                           @NonNull String id,
                           @NonNull SaveCrmDealBuilder deal,
                           @Nullable CrmStageWrite stage) {
        this.update(session, CrmObjectType.CRM_OBJECT_TYPE_DEAL, id,
                HubSpotMapper.deal(deal, null, stage == null ? List.of() : List.of(stage)));
    }

    // ── leads ────────────────────────────────────────────────────────────────

    @Override
    public @NonNull CrmPage<CrmContainedRecord<CrmLead>> listLeads(@NonNull CrmSession session,
                                                                   @NonNull String containerRemoteId,
                                                                   @NonNull ListCrmObjectsBuilder query,
                                                                   @NonNull String stageProperty) {
        return this.list(session, CrmObjectType.CRM_OBJECT_TYPE_LEAD, query,
                Map.of(HubSpotContainerKind.LEADS.pipelineProperty(), containerRemoteId),
                with(HubSpotMapper.LEAD_PROPERTIES, stageProperty), HubSpotMapper::lead);
    }

    @Override
    public @NonNull CrmContainedRecord<CrmLead> getLead(@NonNull CrmSession session,
                                                        @NonNull String id,
                                                        @NonNull String stageProperty) {
        return HubSpotMapper.lead(this.get(session, CrmObjectType.CRM_OBJECT_TYPE_LEAD, id,
                with(HubSpotMapper.LEAD_PROPERTIES, stageProperty)));
    }

    /** HubSpot refuses a lead that is not associated with an existing contact at creation. */
    @Override
    public @NonNull String createLead(@NonNull CrmSession session,
                                      @NonNull String containerRemoteId,
                                      @NonNull CreateCrmLeadBuilder lead,
                                      @NonNull List<CrmStageWrite> stages) {
        var contact = Map.of(
                "to", Map.of("id", lead.contactId()),
                "types", List.of(Map.of(
                        "associationCategory", "HUBSPOT_DEFINED",
                        "associationTypeId", LEAD_TO_CONTACT_ASSOCIATION_TYPE)));
        return this.create(session, CrmObjectType.CRM_OBJECT_TYPE_LEAD,
                HubSpotMapper.lead(lead, containerRemoteId, stages), List.of(contact));
    }

    // ── activities ───────────────────────────────────────────────────────────

    @Override
    public @NonNull CrmPage<CrmActivity> listActivities(@NonNull CrmSession session,
                                                        @NonNull CrmActivityType type,
                                                        @NonNull ListCrmObjectsBuilder query) {
        return this.list(session, HubSpotObjectTypes.of(type), query, null,
                HubSpotMapper.ActivityProperties.of(type).all(), object -> HubSpotMapper.activity(type, object));
    }

    @Override
    public @NonNull CrmActivity getActivity(@NonNull CrmSession session,
                                            @NonNull CrmActivityType type,
                                            @NonNull String id) {
        return HubSpotMapper.activity(type,
                this.get(session, HubSpotObjectTypes.of(type), id, HubSpotMapper.ActivityProperties.of(type).all()));
    }

    @Override
    public @NonNull String createActivity(@NonNull CrmSession session, @NonNull CreateCrmActivityBuilder activity) {
        return this.create(session, HubSpotObjectTypes.of(activity.type()), HubSpotMapper.activity(activity),
                List.of());
    }

    // ── associations ─────────────────────────────────────────────────────────

    @Override
    public void associate(@NonNull CrmSession session, @NonNull CrmObjectRef from, @NonNull CrmObjectRef to) {
        this.clientFactory.create(session).call(HttpMethod.PUT, List.of(
                "crm", "objects", API_VERSION, HubSpotObjectTypes.singular(from.type()), from.id(),
                "associations", "default", HubSpotObjectTypes.singular(to.type()), to.id()));
    }

    /** Removes every association between the two records — HubSpot has no way to remove only the default one. */
    @Override
    public void dissociate(@NonNull CrmSession session, @NonNull CrmObjectRef from, @NonNull CrmObjectRef to) {
        this.clientFactory.create(session).call(HttpMethod.DELETE, List.of(
                "crm", "objects", API_VERSION, HubSpotObjectTypes.singular(from.type()), from.id(),
                "associations", HubSpotObjectTypes.singular(to.type()), to.id()));
    }

    // ── generic object calls ─────────────────────────────────────────────────

    /** @param equals property → value filters every record must match (the pipeline); forces the search. */
    private <T> CrmPage<T> list(@NonNull CrmSession session,
                                @NonNull CrmObjectType type,
                                @NonNull ListCrmObjectsBuilder query,
                                @Nullable Map<String, String> equals,
                                @NonNull List<String> properties,
                                @NonNull Function<HubSpotObject, T> mapper) {
        var client = this.clientFactory.create(session);
        HubSpotResults<HubSpotObject> response;
        if (query.filtered() || equals != null) {
            response = client.search(objectPath(type, "search"), searchBody(query, equals, properties), OBJECTS);
        } else {
            var params = new HashMap<String, String>();
            params.put("limit", String.valueOf(query.limit()));
            params.put("properties", String.join(",", properties));
            if (query.cursor() != null) {
                params.put("after", query.cursor());
            }
            response = client.get(objectPath(type), params, OBJECTS);
        }

        var items = results(response).stream().filter(object -> object.id() != null).map(mapper).toList();
        var next = response != null && response.paging() != null && response.paging().next() != null
                ? response.paging().next().after()
                : null;
        return new CrmPage<>(items, next);
    }

    private HubSpotObject get(@NonNull CrmSession session,
                              @NonNull CrmObjectType type,
                              @NonNull String id,
                              @NonNull List<String> properties) {
        var response = this.clientFactory.create(session)
                .get(objectPath(type, id), Map.of("properties", String.join(",", properties)), OBJECT);
        if (response == null || response.id() == null) {
            throw new NotFoundException("HubSpot: no %s %s".formatted(HubSpotObjectTypes.singular(type), id));
        }
        return response;
    }

    private String create(@NonNull CrmSession session,
                          @NonNull CrmObjectType type,
                          @NonNull Map<String, String> properties,
                          @NonNull List<Map<String, Object>> associations) {
        var body = new HashMap<String, Object>();
        body.put("properties", properties);
        if (!associations.isEmpty()) {
            body.put("associations", associations);
        }
        var response = this.clientFactory.create(session)
                .send(HttpMethod.POST, objectPath(type), body, OBJECT);
        if (response == null || response.id() == null) {
            throw new CrmUpstreamException("HubSpot answered a create without an id");
        }
        return response.id();
    }

    private void update(@NonNull CrmSession session,
                        @NonNull CrmObjectType type,
                        @NonNull String id,
                        @NonNull Map<String, String> properties) {
        this.clientFactory.create(session)
                .send(HttpMethod.PATCH, objectPath(type, id), Map.of("properties", properties), OBJECT);
    }

    private static Map<String, Object> searchBody(@NonNull ListCrmObjectsBuilder query,
                                                  @Nullable Map<String, String> equals,
                                                  @NonNull List<String> properties) {
        var body = new HashMap<String, Object>();
        body.put("limit", query.limit());
        body.put("properties", properties);
        if (query.cursor() != null) {
            body.put("after", query.cursor());
        }
        if (query.q() != null && !query.q().isBlank()) {
            body.put("query", query.q());
        }

        // One filter group: every filter in it must match.
        var filters = new ArrayList<Map<String, String>>();
        if (equals != null) {
            equals.forEach((property, value) ->
                    filters.add(Map.of("propertyName", property, "operator", "EQ", "value", value)));
        }
        if (query.associatedTo() != null) {
            filters.add(Map.of(
                    "propertyName", "associations." + HubSpotObjectTypes.singular(query.associatedTo().type()),
                    "operator", "EQ",
                    "value", query.associatedTo().id()));
        }
        if (!filters.isEmpty()) {
            body.put("filterGroups", List.of(Map.of("filters", filters)));
        }
        return body;
    }

    private static List<String> objectPath(@NonNull CrmObjectType type, @NonNull String... rest) {
        var segments = new ArrayList<>(List.of("crm", "objects", API_VERSION, HubSpotObjectTypes.plural(type)));
        segments.addAll(List.of(rest));
        return segments;
    }

    private static List<String> with(@NonNull List<String> properties, @NonNull String stageProperty) {
        var all = new LinkedHashSet<>(properties);
        all.add(stageProperty);
        return List.copyOf(all);
    }

    private static <T> List<T> results(@Nullable HubSpotResults<T> response) {
        return response == null || response.results() == null ? List.of() : response.results();
    }
}
