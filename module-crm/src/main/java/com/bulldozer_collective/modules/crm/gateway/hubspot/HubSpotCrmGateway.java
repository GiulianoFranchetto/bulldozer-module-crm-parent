package com.bulldozer_collective.modules.crm.gateway.hubspot;

import com.bulldozer_collective.modules.crm.activities.services.CreateCrmActivityBuilder;
import com.bulldozer_collective.modules.crm.api.CrmActivity;
import com.bulldozer_collective.modules.crm.api.CrmActivityType;
import com.bulldozer_collective.modules.crm.api.CrmCompany;
import com.bulldozer_collective.modules.crm.api.CrmContact;
import com.bulldozer_collective.modules.crm.api.CrmDealProperty;
import com.bulldozer_collective.modules.crm.api.CrmObjectRef;
import com.bulldozer_collective.modules.crm.api.CrmObjectType;
import com.bulldozer_collective.modules.crm.api.CrmPage;
import com.bulldozer_collective.modules.crm.api.CrmPipeline;
import com.bulldozer_collective.modules.crm.api.CrmPipelineStage;
import com.bulldozer_collective.modules.crm.companies.services.SaveCrmCompanyBuilder;
import com.bulldozer_collective.modules.crm.contacts.services.SaveCrmContactBuilder;
import com.bulldozer_collective.modules.crm.deals.services.SaveCrmDealBuilder;
import com.bulldozer_collective.modules.crm.gateway.CrmDealRecord;
import com.bulldozer_collective.modules.crm.gateway.CrmGateway;
import com.bulldozer_collective.modules.crm.gateway.CrmSession;
import com.bulldozer_collective.modules.crm.gateway.CrmStageWrite;
import com.bulldozer_collective.modules.crm.gateway.ListCrmObjectsBuilder;
import com.bulldozer_collective.modules.crm.gateway.hubspot.dto.HubSpotObject;
import com.bulldozer_collective.modules.crm.gateway.hubspot.dto.HubSpotPipeline;
import com.bulldozer_collective.modules.crm.gateway.hubspot.dto.HubSpotProperty;
import com.bulldozer_collective.modules.crm.gateway.hubspot.dto.HubSpotResults;
import com.bulldozer_collective.shared.IntegrationPlatform;
import com.bulldozer_collective.shared.exceptions.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import static com.bulldozer_collective.modules.crm.gateway.hubspot.HubSpotClient.API_VERSION;

/**
 * HubSpot implementation of {@link CrmGateway}, on the {@value HubSpotClient#API_VERSION} CRM API.
 *
 * <p>A plain listing goes through {@code GET /crm/objects/{version}/{type}}; a full-text query or an association
 * filter goes through {@code POST …/{type}/search}, which HubSpot indexes with a few seconds' delay and limits to
 * five requests per second and 10,000 results per query. Associations are created with HubSpot's default
 * (unlabelled) association type, which needs no association type id.
 */
@Component
@RequiredArgsConstructor
public class HubSpotCrmGateway implements CrmGateway {

    private static final TypeReference<HubSpotResults<HubSpotObject>> OBJECTS = new TypeReference<>() {};
    private static final TypeReference<HubSpotObject> OBJECT = new TypeReference<>() {};
    private static final TypeReference<HubSpotResults<HubSpotPipeline>> PIPELINES = new TypeReference<>() {};
    private static final TypeReference<HubSpotResults<HubSpotProperty>> PROPERTIES = new TypeReference<>() {};

    /** HubSpot's object type id for deals, as the pipelines endpoint is documented with it. */
    private static final String DEALS_TYPE_ID = "0-3";

    private final HubSpotClientFactory clientFactory;

    @Override
    public @NonNull IntegrationPlatform platform() {
        return IntegrationPlatform.INTEGRATION_PLATFORM_HUBSPOT;
    }

    // ── contacts ─────────────────────────────────────────────────────────────

    @Override
    public @NonNull CrmPage<CrmContact> listContacts(@NonNull CrmSession session,
                                                     @NonNull ListCrmObjectsBuilder query) {
        return this.list(session, CrmObjectType.CRM_OBJECT_TYPE_CONTACT, query, HubSpotMapper.CONTACT_PROPERTIES,
                HubSpotMapper::contact);
    }

    @Override
    public @NonNull CrmContact getContact(@NonNull CrmSession session, @NonNull String id) {
        return HubSpotMapper.contact(
                this.get(session, CrmObjectType.CRM_OBJECT_TYPE_CONTACT, id, HubSpotMapper.CONTACT_PROPERTIES));
    }

    @Override
    public @NonNull String createContact(@NonNull CrmSession session, @NonNull SaveCrmContactBuilder contact) {
        return this.create(session, CrmObjectType.CRM_OBJECT_TYPE_CONTACT, HubSpotMapper.contact(contact));
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
        return this.list(session, CrmObjectType.CRM_OBJECT_TYPE_COMPANY, query, HubSpotMapper.COMPANY_PROPERTIES,
                HubSpotMapper::company);
    }

    @Override
    public @NonNull CrmCompany getCompany(@NonNull CrmSession session, @NonNull String id) {
        return HubSpotMapper.company(
                this.get(session, CrmObjectType.CRM_OBJECT_TYPE_COMPANY, id, HubSpotMapper.COMPANY_PROPERTIES));
    }

    @Override
    public @NonNull String createCompany(@NonNull CrmSession session, @NonNull SaveCrmCompanyBuilder company) {
        return this.create(session, CrmObjectType.CRM_OBJECT_TYPE_COMPANY, HubSpotMapper.company(company));
    }

    @Override
    public void updateCompany(@NonNull CrmSession session,
                              @NonNull String id,
                              @NonNull SaveCrmCompanyBuilder company) {
        this.update(session, CrmObjectType.CRM_OBJECT_TYPE_COMPANY, id, HubSpotMapper.company(company));
    }

    // ── deals ────────────────────────────────────────────────────────────────

    @Override
    public @NonNull CrmPage<CrmDealRecord> listDeals(@NonNull CrmSession session,
                                                     @NonNull ListCrmObjectsBuilder query,
                                                     @NonNull Collection<String> stageProperties) {
        return this.list(session, CrmObjectType.CRM_OBJECT_TYPE_DEAL, query, dealProperties(stageProperties),
                HubSpotMapper::deal);
    }

    @Override
    public @NonNull CrmDealRecord getDeal(@NonNull CrmSession session,
                                          @NonNull String id,
                                          @NonNull Collection<String> stageProperties) {
        return HubSpotMapper.deal(
                this.get(session, CrmObjectType.CRM_OBJECT_TYPE_DEAL, id, dealProperties(stageProperties)));
    }

    @Override
    public @NonNull String createDeal(@NonNull CrmSession session,
                                      @NonNull SaveCrmDealBuilder deal,
                                      @NonNull CrmStageWrite stage) {
        return this.create(session, CrmObjectType.CRM_OBJECT_TYPE_DEAL, HubSpotMapper.deal(deal, stage));
    }

    @Override
    public void updateDeal(@NonNull CrmSession session,
                           @NonNull String id,
                           @NonNull SaveCrmDealBuilder deal,
                           @Nullable CrmStageWrite stage) {
        this.update(session, CrmObjectType.CRM_OBJECT_TYPE_DEAL, id, HubSpotMapper.deal(deal, stage));
    }

    @Override
    public @NonNull List<CrmPipeline> listPipelines(@NonNull CrmSession session) {
        var response = this.clientFactory.create(session.accessToken())
                .get(List.of("crm", "pipelines", API_VERSION, DEALS_TYPE_ID), Map.of(), PIPELINES);
        return results(response).stream()
                .filter(pipeline -> pipeline.id() != null && !Boolean.TRUE.equals(pipeline.archived()))
                .map(HubSpotMapper::pipeline)
                .sorted(Comparator.comparingInt(CrmPipeline::displayOrder))
                .toList();
    }

    @Override
    public @NonNull List<CrmDealProperty> listDealProperties(@NonNull CrmSession session) {
        var response = this.clientFactory.create(session.accessToken())
                .get(List.of("crm", "properties", API_VERSION, "deals"), Map.of(), PROPERTIES);
        return results(response).stream()
                .filter(property -> property.name() != null
                        && !Boolean.TRUE.equals(property.archived())
                        && !Boolean.TRUE.equals(property.hidden()))
                .map(HubSpotMapper::property)
                .sorted(Comparator.comparing(CrmDealProperty::label, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    /**
     * The native {@code dealstage} takes the stage ids of the deal's own pipeline — its property definition lists
     * the stages of every pipeline, so it cannot be used. Any other property takes the values of its options.
     */
    @Override
    public @NonNull List<String> listStageValues(@NonNull CrmSession session,
                                                 @NonNull String pipelineId,
                                                 @NonNull String propertyName) {
        if (HubSpotMapper.DEAL_STAGE.equals(propertyName)) {
            return this.listPipelines(session).stream()
                    .filter(pipeline -> pipeline.id().equals(pipelineId))
                    .findFirst()
                    .map(pipeline -> pipeline.stages().stream().map(CrmPipelineStage::id).toList())
                    .orElseThrow(() -> new NotFoundException("HubSpot: no deal pipeline " + pipelineId));
        }
        return this.listDealProperties(session).stream()
                .filter(property -> property.name().equals(propertyName))
                .findFirst()
                .map(property -> property.options().stream().map(option -> option.value()).toList())
                .orElseThrow(() -> new IllegalArgumentException(
                        "propertyName: '%s' is not a deal property of this HubSpot account".formatted(propertyName)));
    }

    // ── activities ───────────────────────────────────────────────────────────

    @Override
    public @NonNull CrmPage<CrmActivity> listActivities(@NonNull CrmSession session,
                                                        @NonNull CrmActivityType type,
                                                        @NonNull ListCrmObjectsBuilder query) {
        return this.list(session, HubSpotObjectTypes.of(type), query,
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
        return this.create(session, HubSpotObjectTypes.of(activity.type()), HubSpotMapper.activity(activity));
    }

    // ── associations ─────────────────────────────────────────────────────────

    @Override
    public void associate(@NonNull CrmSession session, @NonNull CrmObjectRef from, @NonNull CrmObjectRef to) {
        this.clientFactory.create(session.accessToken()).call(HttpMethod.PUT, List.of(
                "crm", "objects", API_VERSION, HubSpotObjectTypes.singular(from.type()), from.id(),
                "associations", "default", HubSpotObjectTypes.singular(to.type()), to.id()));
    }

    /** Removes every association between the two records — HubSpot has no way to remove only the default one. */
    @Override
    public void dissociate(@NonNull CrmSession session, @NonNull CrmObjectRef from, @NonNull CrmObjectRef to) {
        this.clientFactory.create(session.accessToken()).call(HttpMethod.DELETE, List.of(
                "crm", "objects", API_VERSION, HubSpotObjectTypes.singular(from.type()), from.id(),
                "associations", HubSpotObjectTypes.singular(to.type()), to.id()));
    }

    // ── generic object calls ─────────────────────────────────────────────────

    private <T> CrmPage<T> list(@NonNull CrmSession session,
                                @NonNull CrmObjectType type,
                                @NonNull ListCrmObjectsBuilder query,
                                @NonNull List<String> properties,
                                @NonNull Function<HubSpotObject, T> mapper) {
        var client = this.clientFactory.create(session.accessToken());
        HubSpotResults<HubSpotObject> response;
        if (query.filtered()) {
            response = client.send(HttpMethod.POST, objectPath(type, "search"), searchBody(query, properties), OBJECTS);
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
        var response = this.clientFactory.create(session.accessToken())
                .get(objectPath(type, id), Map.of("properties", String.join(",", properties)), OBJECT);
        if (response == null || response.id() == null) {
            throw new NotFoundException("HubSpot: no %s %s".formatted(HubSpotObjectTypes.singular(type), id));
        }
        return response;
    }

    private String create(@NonNull CrmSession session,
                          @NonNull CrmObjectType type,
                          @NonNull Map<String, String> properties) {
        var response = this.clientFactory.create(session.accessToken())
                .send(HttpMethod.POST, objectPath(type), Map.of("properties", properties), OBJECT);
        if (response == null || response.id() == null) {
            throw new IllegalStateException("HubSpot answered a create without an id");
        }
        return response.id();
    }

    private void update(@NonNull CrmSession session,
                        @NonNull CrmObjectType type,
                        @NonNull String id,
                        @NonNull Map<String, String> properties) {
        this.clientFactory.create(session.accessToken())
                .send(HttpMethod.PATCH, objectPath(type, id), Map.of("properties", properties), OBJECT);
    }

    private static Map<String, Object> searchBody(@NonNull ListCrmObjectsBuilder query,
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
        if (query.associatedTo() != null) {
            var filter = Map.of(
                    "propertyName", "associations." + HubSpotObjectTypes.singular(query.associatedTo().type()),
                    "operator", "EQ",
                    "value", query.associatedTo().id());
            body.put("filterGroups", List.of(Map.of("filters", List.of(filter))));
        }
        return body;
    }

    private static List<String> objectPath(@NonNull CrmObjectType type, @NonNull String... rest) {
        var segments = new ArrayList<>(List.of("crm", "objects", API_VERSION, HubSpotObjectTypes.plural(type)));
        segments.addAll(List.of(rest));
        return segments;
    }

    private static List<String> dealProperties(@NonNull Collection<String> stageProperties) {
        var properties = new LinkedHashSet<>(HubSpotMapper.DEAL_PROPERTIES);
        stageProperties.stream().filter(Objects::nonNull).forEach(properties::add);
        return List.copyOf(properties);
    }

    private static <T> List<T> results(@Nullable HubSpotResults<T> response) {
        return response == null || response.results() == null ? List.of() : response.results();
    }
}
