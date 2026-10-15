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
import com.bulldozer_collective.modules.crm.containers.services.CreateCrmPropertyBuilder;
import com.bulldozer_collective.modules.crm.deals.services.SaveCrmDealBuilder;
import com.bulldozer_collective.modules.crm.gateway.CrmBulkResult;
import com.bulldozer_collective.modules.crm.gateway.CrmContainedRecord;
import com.bulldozer_collective.modules.crm.gateway.CrmGateway;
import com.bulldozer_collective.modules.crm.gateway.CrmLink;
import com.bulldozer_collective.modules.crm.gateway.CrmNote;
import com.bulldozer_collective.modules.crm.gateway.CrmSession;
import com.bulldozer_collective.modules.crm.gateway.CrmStageOption;
import com.bulldozer_collective.modules.crm.gateway.CrmStageWrite;
import com.bulldozer_collective.modules.crm.gateway.CrmUpstreamException;
import com.bulldozer_collective.modules.crm.gateway.ListCrmObjectsBuilder;
import com.bulldozer_collective.modules.crm.gateway.hubspot.dto.HubSpotAssociationRow;
import com.bulldozer_collective.modules.crm.gateway.hubspot.dto.HubSpotBatchResponse;
import com.bulldozer_collective.modules.crm.gateway.hubspot.dto.HubSpotObject;
import com.bulldozer_collective.modules.crm.gateway.hubspot.dto.HubSpotPipeline;
import com.bulldozer_collective.modules.crm.gateway.hubspot.dto.HubSpotProperty;
import com.bulldozer_collective.modules.crm.gateway.hubspot.dto.HubSpotResults;
import com.bulldozer_collective.modules.crm.leads.services.CreateCrmLeadBuilder;
import com.bulldozer_collective.shared.IntegrationPlatform;
import com.bulldozer_collective.shared.exceptions.BadRequestException;
import com.bulldozer_collective.shared.exceptions.ConflictException;
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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
    private static final TypeReference<HubSpotProperty> PROPERTY = new TypeReference<>() {};
    private static final TypeReference<HubSpotResults<HubSpotProperty>> PROPERTIES = new TypeReference<>() {};
    private static final TypeReference<HubSpotBatchResponse<HubSpotObject>> BATCH_OBJECTS = new TypeReference<>() {};
    private static final TypeReference<HubSpotBatchResponse<HubSpotAssociationRow>> BATCH_ASSOCIATIONS =
            new TypeReference<>() {};

    /** HubSpot's batch endpoints take at most 100 inputs (Object APIs guide, "Limits"). */
    private static final int BATCH_SIZE = 100;

    /**
     * The note → contact and note → company associations a note is created with ({@code HUBSPOT_DEFINED}). 202 is
     * the notes guide's example; 190 is HubSpot's default note → company type.
     */
    private static final int NOTE_TO_CONTACT_ASSOCIATION_TYPE = 202;
    private static final int NOTE_TO_COMPANY_ASSOCIATION_TYPE = 190;

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
    public @NonNull List<CrmProperty> listProperties(@NonNull CrmSession session, @NonNull CrmObjectType type) {
        var response = this.clientFactory.create(session)
                .get(List.of("crm", "properties", API_VERSION, HubSpotObjectTypes.plural(type)), Map.of(), PROPERTIES);
        return results(response).stream()
                .filter(property -> property.name() != null
                        && !Boolean.TRUE.equals(property.archived())
                        && !Boolean.TRUE.equals(property.hidden()))
                .map(HubSpotMapper::property)
                .sorted(Comparator.comparing(CrmProperty::label, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Override
    public @NonNull CrmProperty createProperty(@NonNull CrmSession session, @NonNull CreateCrmPropertyBuilder property) {
        var created = this.clientFactory.create(session).send(HttpMethod.POST,
                List.of("crm", "properties", API_VERSION, HubSpotObjectTypes.plural(property.objectType())),
                HubSpotMapper.property(property), PROPERTY);
        return HubSpotMapper.property(Objects.requireNonNull(created));
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
        return this.listProperties(session, kind.objectType()).stream()
                .filter(property -> property.name().equals(propertyName))
                .findFirst()
                .map(property -> {
                    // A calculated or HubSpot-managed property reads fine, but every stage write to it would fail.
                    if (property.readOnly()) {
                        throw new IllegalArgumentException(("stagePropertyName: '%s' is read-only in HubSpot, so "
                                + "Bulldozer could not write a stage to it; choose a writable property")
                                .formatted(propertyName));
                    }
                    return property;
                })
                .map(property -> property.options().stream()
                        .filter(option -> !option.hidden())
                        .map(option -> new CrmStageOption(option.value(), option.label(), option.displayOrder()))
                        .toList())
                .orElseThrow(() -> new IllegalArgumentException(
                        "stagePropertyName: '%s' is not a property of this HubSpot account".formatted(propertyName)));
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
        return this.list(session, type.objectType(), query, null,
                HubSpotMapper.ActivityProperties.of(type).all(), object -> HubSpotMapper.activity(type, object));
    }

    @Override
    public @NonNull CrmActivity getActivity(@NonNull CrmSession session,
                                            @NonNull CrmActivityType type,
                                            @NonNull String id) {
        return HubSpotMapper.activity(type,
                this.get(session, type.objectType(), id, HubSpotMapper.ActivityProperties.of(type).all()));
    }

    @Override
    public @NonNull String createActivity(@NonNull CrmSession session, @NonNull CreateCrmActivityBuilder activity) {
        return this.create(session, activity.type().objectType(), HubSpotMapper.activity(activity),
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

    // ── bulk, for imports ────────────────────────────────────────────────────

    /** A batch read by the {@code email} unique property: an email no contact holds comes back as an error, ignored. */
    @Override
    public @NonNull List<CrmContact> findContactsByEmail(@NonNull CrmSession session,
                                                         @NonNull Collection<String> emails) {
        var client = this.clientFactory.create(session);
        var found = new ArrayList<CrmContact>();
        for (var chunk : chunks(List.copyOf(emails))) {
            var body = Map.of(
                    "idProperty", "email",
                    "inputs", chunk.stream().map(email -> Map.of("id", email)).toList(),
                    "properties", HubSpotMapper.CONTACT_PROPERTIES,
                    "propertiesWithHistory", List.of());
            try {
                var response = client.search(objectPath(CrmObjectType.CRM_OBJECT_TYPE_CONTACT, "batch", "read"),
                        body, BATCH_OBJECTS);
                batchResults(response).forEach(object -> found.add(HubSpotMapper.contact(object)));
            } catch (NotFoundException e) {
                // None of the chunk's emails is known: nothing found, not an error.
            }
        }
        return found;
    }

    @Override
    public @NonNull List<CrmContact> findContactsByName(@NonNull CrmSession session,
                                                        @NonNull String firstName,
                                                        @NonNull String lastName) {
        return this.searchAll(session, CrmObjectType.CRM_OBJECT_TYPE_CONTACT,
                        List.of(equal("firstname", firstName), equal("lastname", lastName)),
                        HubSpotMapper.CONTACT_PROPERTIES).stream()
                .map(HubSpotMapper::contact)
                .toList();
    }

    @Override
    public @NonNull List<CrmCompany> findCompaniesByDomain(@NonNull CrmSession session,
                                                           @NonNull Collection<String> domains) {
        var found = new ArrayList<CrmCompany>();
        for (var chunk : chunks(List.copyOf(domains))) {
            this.searchAll(session, CrmObjectType.CRM_OBJECT_TYPE_COMPANY,
                            List.of(Map.of("propertyName", "domain", "operator", "IN", "values", chunk)),
                            HubSpotMapper.COMPANY_PROPERTIES)
                    .forEach(object -> found.add(HubSpotMapper.company(object)));
        }
        return found;
    }

    @Override
    public @NonNull List<CrmCompany> findCompaniesByName(@NonNull CrmSession session, @NonNull String name) {
        return this.searchAll(session, CrmObjectType.CRM_OBJECT_TYPE_COMPANY, List.of(equal("name", name)),
                        HubSpotMapper.COMPANY_PROPERTIES).stream()
                .map(HubSpotMapper::company)
                .toList();
    }

    @Override
    public @NonNull CrmBulkResult<CrmContact> createContacts(@NonNull CrmSession session,
                                                            @NonNull List<SaveCrmContactBuilder> contacts) {
        var result = this.writeAll(session, CrmObjectType.CRM_OBJECT_TYPE_CONTACT, "create", contacts.stream()
                .map(contact -> Map.<String, Object>of("properties", HubSpotMapper.contact(contact)))
                .toList());
        return new CrmBulkResult<>(result.done().stream().map(HubSpotMapper::contact).toList(), result.failures());
    }

    @Override
    public @NonNull CrmBulkResult<String> updateContacts(@NonNull CrmSession session,
                                                         @NonNull List<String> ids,
                                                         @NonNull List<SaveCrmContactBuilder> contacts) {
        return ids(this.writeAll(session, CrmObjectType.CRM_OBJECT_TYPE_CONTACT, "update",
                updates(ids, contacts.stream().map(HubSpotMapper::contact).toList())));
    }

    @Override
    public @NonNull CrmBulkResult<CrmCompany> createCompanies(@NonNull CrmSession session,
                                                             @NonNull List<SaveCrmCompanyBuilder> companies) {
        var result = this.writeAll(session, CrmObjectType.CRM_OBJECT_TYPE_COMPANY, "create", companies.stream()
                .map(company -> Map.<String, Object>of("properties", HubSpotMapper.company(company)))
                .toList());
        return new CrmBulkResult<>(result.done().stream().map(HubSpotMapper::company).toList(), result.failures());
    }

    @Override
    public @NonNull CrmBulkResult<String> updateCompanies(@NonNull CrmSession session,
                                                          @NonNull List<String> ids,
                                                          @NonNull List<SaveCrmCompanyBuilder> companies) {
        return ids(this.writeAll(session, CrmObjectType.CRM_OBJECT_TYPE_COMPANY, "update",
                updates(ids, companies.stream().map(HubSpotMapper::company).toList())));
    }

    @Override
    public @NonNull Map<String, Set<String>> listAssociatedIds(@NonNull CrmSession session,
                                                               @NonNull CrmObjectType from,
                                                               @NonNull Collection<String> fromIds,
                                                               @NonNull CrmObjectType to) {
        var client = this.clientFactory.create(session);
        var associated = new HashMap<String, Set<String>>();
        for (var chunk : chunks(List.copyOf(fromIds))) {
            var body = Map.of("inputs", chunk.stream().map(id -> Map.of("id", id)).toList());
            var response = client.search(associationsPath(from, to, "batch", "read"), body, BATCH_ASSOCIATIONS);
            for (var row : batchResults(response)) {
                if (row.from() == null || row.from().id() == null || row.to() == null) {
                    continue;
                }
                var targets = associated.computeIfAbsent(row.from().id(), id -> new HashSet<>());
                row.to().stream()
                        .filter(target -> target.toObjectId() != null)
                        .forEach(target -> targets.add(String.valueOf(target.toObjectId())));
            }
        }
        return associated;
    }

    @Override
    public @NonNull List<String> associateAll(@NonNull CrmSession session,
                                              @NonNull CrmObjectType from,
                                              @NonNull CrmObjectType to,
                                              @NonNull List<CrmLink> links) {
        var client = this.clientFactory.create(session);
        var errors = new ArrayList<String>();
        for (var chunk : chunks(links)) {
            var body = Map.of("inputs", chunk.stream()
                    .map(link -> Map.of("from", Map.of("id", link.fromId()), "to", Map.of("id", link.toId())))
                    .toList());
            try {
                var response = client.send(HttpMethod.POST, associationsPath(from, to, "batch", "associate", "default"),
                        body, BATCH_OBJECTS);
                if (response != null && response.errors() != null) {
                    response.errors().forEach(error -> errors.add("HubSpot: " + error.message()));
                }
            } catch (BadRequestException | ConflictException | NotFoundException e) {
                errors.add(e.getMessage());
            }
        }
        return errors;
    }

    @Override
    public @NonNull CrmBulkResult<String> createNotes(@NonNull CrmSession session, @NonNull List<CrmNote> notes) {
        return ids(this.writeAll(session, CrmObjectType.CRM_OBJECT_TYPE_NOTE, "create", notes.stream()
                .map(note -> Map.<String, Object>of(
                        "properties", HubSpotMapper.note(note),
                        "associations", List.of(Map.of(
                                "to", Map.of("id", note.about().id()),
                                "types", List.of(Map.of(
                                        "associationCategory", "HUBSPOT_DEFINED",
                                        "associationTypeId", noteAssociationType(note.about().type())))))))
                .toList()));
    }

    /**
     * Sends {@code inputs} to {@code batch/<operation>} by batches of 100. Each input carries its index as
     * {@code objectWriteTraceId}, which makes HubSpot answer a partial failure with a {@code 207} naming the refused
     * inputs instead of failing them all. A batch HubSpot refuses as a whole fails each of its inputs.
     */
    private CrmBulkResult<HubSpotObject> writeAll(@NonNull CrmSession session,
                                                  @NonNull CrmObjectType type,
                                                  @NonNull String operation,
                                                  @NonNull List<Map<String, Object>> inputs) {
        var client = this.clientFactory.create(session);
        var done = new ArrayList<HubSpotObject>();
        var failures = new HashMap<Integer, String>();
        for (int start = 0; start < inputs.size(); start += BATCH_SIZE) {
            var end = Math.min(start + BATCH_SIZE, inputs.size());
            var batch = new ArrayList<Map<String, Object>>();
            for (int index = start; index < end; index++) {
                var input = new HashMap<>(inputs.get(index));
                input.put("objectWriteTraceId", String.valueOf(index));
                batch.add(input);
            }
            try {
                // POST: a batch create is never replayed — HubSpot may have written it already.
                var response = client.send(HttpMethod.POST, objectPath(type, "batch", operation),
                        Map.of("inputs", batch), BATCH_OBJECTS);
                done.addAll(batchResults(response));
                failures.putAll(batchFailures(response));
            } catch (BadRequestException | ConflictException | NotFoundException e) {
                for (int index = start; index < end; index++) {
                    failures.put(index, e.getMessage());
                }
            }
        }
        return new CrmBulkResult<>(done, failures);
    }

    /** Every record matching all {@code filters}, across HubSpot's search pages (capped at 10,000 by HubSpot). */
    private List<HubSpotObject> searchAll(@NonNull CrmSession session,
                                          @NonNull CrmObjectType type,
                                          @NonNull List<Map<String, Object>> filters,
                                          @NonNull List<String> properties) {
        var client = this.clientFactory.create(session);
        var found = new ArrayList<HubSpotObject>();
        String after = null;
        do {
            var body = new HashMap<String, Object>();
            body.put("filterGroups", List.of(Map.of("filters", filters)));
            body.put("properties", properties);
            body.put("limit", BATCH_SIZE);
            if (after != null) {
                body.put("after", after);
            }
            var response = client.search(objectPath(type, "search"), body, OBJECTS);
            results(response).stream().filter(object -> object.id() != null).forEach(found::add);
            after = response != null && response.paging() != null && response.paging().next() != null
                    ? response.paging().next().after()
                    : null;
        } while (after != null);
        return found;
    }

    private static Map<String, Object> equal(@NonNull String property, @NonNull String value) {
        return Map.of("propertyName", property, "operator", "EQ", "value", value);
    }

    private static List<Map<String, Object>> updates(@NonNull List<String> ids,
                                                     @NonNull List<Map<String, String>> properties) {
        var inputs = new ArrayList<Map<String, Object>>();
        for (int index = 0; index < ids.size(); index++) {
            inputs.add(Map.of("id", ids.get(index), "properties", properties.get(index)));
        }
        return inputs;
    }

    private static CrmBulkResult<String> ids(@NonNull CrmBulkResult<HubSpotObject> result) {
        return new CrmBulkResult<>(result.done().stream()
                .map(HubSpotObject::id)
                .filter(Objects::nonNull)
                .toList(), result.failures());
    }

    private static int noteAssociationType(@NonNull CrmObjectType type) {
        return switch (type) {
            case CRM_OBJECT_TYPE_CONTACT -> NOTE_TO_CONTACT_ASSOCIATION_TYPE;
            case CRM_OBJECT_TYPE_COMPANY -> NOTE_TO_COMPANY_ASSOCIATION_TYPE;
            default -> throw new IllegalArgumentException("A note is logged on a contact or a company, not " + type);
        };
    }

    private static <T> List<List<T>> chunks(@NonNull List<T> items) {
        var chunks = new ArrayList<List<T>>();
        for (int start = 0; start < items.size(); start += BATCH_SIZE) {
            chunks.add(items.subList(start, Math.min(start + BATCH_SIZE, items.size())));
        }
        return chunks;
    }

    private static <T> List<T> batchResults(@Nullable HubSpotBatchResponse<T> response) {
        return response == null || response.results() == null ? List.of() : response.results();
    }

    /** The refused inputs of a {@code 207}, by the index their {@code objectWriteTraceId} carries. */
    private static Map<Integer, String> batchFailures(@Nullable HubSpotBatchResponse<?> response) {
        var failures = new HashMap<Integer, String>();
        if (response == null || response.errors() == null) {
            return failures;
        }
        for (var error : response.errors()) {
            var traces = error.context() == null ? null : error.context().get("objectWriteTraceId");
            if (traces == null) {
                continue;
            }
            traces.forEach(trace -> failures.put(Integer.valueOf(trace), "HubSpot: " + error.message()));
        }
        return failures;
    }

    private static List<String> associationsPath(@NonNull CrmObjectType from,
                                                 @NonNull CrmObjectType to,
                                                 @NonNull String... rest) {
        var segments = new ArrayList<>(List.of("crm", "associations", API_VERSION,
                HubSpotObjectTypes.plural(from), HubSpotObjectTypes.plural(to)));
        segments.addAll(List.of(rest));
        return segments;
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
