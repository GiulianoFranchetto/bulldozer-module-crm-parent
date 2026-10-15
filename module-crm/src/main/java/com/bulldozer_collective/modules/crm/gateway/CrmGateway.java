package com.bulldozer_collective.modules.crm.gateway;

import com.bulldozer_collective.modules.crm.activities.services.CreateCrmActivityBuilder;
import com.bulldozer_collective.modules.crm.api.CrmActivity;
import com.bulldozer_collective.modules.crm.api.CrmActivityType;
import com.bulldozer_collective.modules.crm.api.CrmCompany;
import com.bulldozer_collective.modules.crm.api.CrmContact;
import com.bulldozer_collective.modules.crm.api.CrmContainerType;
import com.bulldozer_collective.modules.crm.api.CrmObjectType;
import com.bulldozer_collective.modules.crm.api.CrmDeal;
import com.bulldozer_collective.modules.crm.api.CrmLead;
import com.bulldozer_collective.modules.crm.api.CrmObjectRef;
import com.bulldozer_collective.modules.crm.api.CrmPage;
import com.bulldozer_collective.modules.crm.api.CrmProperty;
import com.bulldozer_collective.modules.crm.companies.services.SaveCrmCompanyBuilder;
import com.bulldozer_collective.modules.crm.contacts.services.SaveCrmContactBuilder;
import com.bulldozer_collective.modules.crm.containers.services.CreateCrmPropertyBuilder;
import com.bulldozer_collective.modules.crm.deals.services.SaveCrmDealBuilder;
import com.bulldozer_collective.modules.crm.leads.services.CreateCrmLeadBuilder;
import com.bulldozer_collective.modules.crm.api.CrmForm;
import com.bulldozer_collective.modules.crm.api.CrmLandingPage;
import com.bulldozer_collective.modules.crm.forms.services.SaveCrmFormBuilder;
import com.bulldozer_collective.modules.crm.landingpages.services.SaveCrmLandingPageBuilder;
import com.bulldozer_collective.shared.IntegrationPlatform;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Façade over one CRM. Each implementation translates between the module's own model ({@code crm-api}) and the
 * CRM's API, and stores nothing: every call goes to the CRM.
 *
 * <p>Ids are the CRM's own. A <b>container</b> is the CRM object grouping deals or leads and their stages — a
 * HubSpot pipeline, a Salesforce sales or lead process — identified here by its CRM id ({@code containerRemoteId}).
 *
 * <p>Writes return the id of the record, never the record: the services read it back, so a write answers with what
 * the CRM actually holds. Implementations translate the CRM's errors: a missing record is a
 * {@code NotFoundException}, a rejected write a {@code BadRequestException} or a {@code ConflictException} carrying
 * the CRM's message, a refused token a {@link CrmAuthorizationException}.
 */
public interface CrmGateway {

    @NonNull IntegrationPlatform platform();

    // ── contacts ─────────────────────────────────────────────────────────────

    @NonNull CrmPage<CrmContact> listContacts(@NonNull CrmSession session, @NonNull ListCrmObjectsBuilder query);

    @NonNull CrmContact getContact(@NonNull CrmSession session, @NonNull String id);

    @NonNull String createContact(@NonNull CrmSession session, @NonNull SaveCrmContactBuilder contact);

    void updateContact(@NonNull CrmSession session, @NonNull String id, @NonNull SaveCrmContactBuilder contact);

    // ── companies ────────────────────────────────────────────────────────────

    @NonNull CrmPage<CrmCompany> listCompanies(@NonNull CrmSession session, @NonNull ListCrmObjectsBuilder query);

    @NonNull CrmCompany getCompany(@NonNull CrmSession session, @NonNull String id);

    @NonNull String createCompany(@NonNull CrmSession session, @NonNull SaveCrmCompanyBuilder company);

    void updateCompany(@NonNull CrmSession session, @NonNull String id, @NonNull SaveCrmCompanyBuilder company);

    // ── containers ───────────────────────────────────────────────────────────

    /** The CRM's containers grouping that type of record, with their native stages. */
    @NonNull List<CrmRemoteContainer> listRemoteContainers(@NonNull CrmSession session, @NonNull CrmContainerType type);

    /** The CRM's native stage property for that type of record — the default stage source. */
    @NonNull String nativeStageProperty(@NonNull CrmContainerType type);

    /**
     * The properties of a type of CRM record: for deals and leads, the candidates for a stage source; for contacts, the
     * options of a field such as the legal basis.
     */
    @NonNull List<CrmProperty> listProperties(@NonNull CrmSession session, @NonNull CrmObjectType type);

    /** Creates the property on every record of its type, and answers it as the CRM now lists it. */
    @NonNull CrmProperty createProperty(@NonNull CrmSession session, @NonNull CreateCrmPropertyBuilder property);

    /**
     * The values {@code propertyName} can take in the container — empty when the property is free-form.
     *
     * @throws IllegalArgumentException when the CRM has no such property, or it is read-only: a stage could never be
     *                                  written to it.
     */
    @NonNull List<CrmStageOption> listStageOptions(@NonNull CrmSession session,
                                                   @NonNull CrmContainerType type,
                                                   @NonNull String containerRemoteId,
                                                   @NonNull String propertyName);

    // ── deals ────────────────────────────────────────────────────────────────

    /** @param stageProperty the container's stage source, read on every deal. */
    @NonNull CrmPage<CrmContainedRecord<CrmDeal>> listDeals(@NonNull CrmSession session,
                                                            @NonNull String containerRemoteId,
                                                            @NonNull ListCrmObjectsBuilder query,
                                                            @NonNull String stageProperty);

    @NonNull CrmContainedRecord<CrmDeal> getDeal(@NonNull CrmSession session,
                                                 @NonNull String id,
                                                 @NonNull String stageProperty);

    /** @param stages every stage value the deal is created with (see {@code CrmStageSources#creationWrites}). */
    @NonNull String createDeal(@NonNull CrmSession session,
                               @NonNull String containerRemoteId,
                               @NonNull SaveCrmDealBuilder deal,
                               @NonNull List<CrmStageWrite> stages);

    void updateDeal(@NonNull CrmSession session,
                    @NonNull String id,
                    @NonNull SaveCrmDealBuilder deal,
                    @Nullable CrmStageWrite stage);

    // ── leads ────────────────────────────────────────────────────────────────

    /** @param stageProperty the container's stage source, read on every lead. */
    @NonNull CrmPage<CrmContainedRecord<CrmLead>> listLeads(@NonNull CrmSession session,
                                                            @NonNull String containerRemoteId,
                                                            @NonNull ListCrmObjectsBuilder query,
                                                            @NonNull String stageProperty);

    @NonNull CrmContainedRecord<CrmLead> getLead(@NonNull CrmSession session,
                                                 @NonNull String id,
                                                 @NonNull String stageProperty);

    /** @param stages every stage value the lead is created with (see {@code CrmStageSources#creationWrites}). */
    @NonNull String createLead(@NonNull CrmSession session,
                               @NonNull String containerRemoteId,
                               @NonNull CreateCrmLeadBuilder lead,
                               @NonNull List<CrmStageWrite> stages);

    // ── activities ───────────────────────────────────────────────────────────

    @NonNull CrmPage<CrmActivity> listActivities(@NonNull CrmSession session,
                                                 @NonNull CrmActivityType type,
                                                 @NonNull ListCrmObjectsBuilder query);

    @NonNull CrmActivity getActivity(@NonNull CrmSession session, @NonNull CrmActivityType type, @NonNull String id);

    @NonNull String createActivity(@NonNull CrmSession session, @NonNull CreateCrmActivityBuilder activity);

    // ── associations ─────────────────────────────────────────────────────────

    void associate(@NonNull CrmSession session, @NonNull CrmObjectRef from, @NonNull CrmObjectRef to);

    void dissociate(@NonNull CrmSession session, @NonNull CrmObjectRef from, @NonNull CrmObjectRef to);

    // ── landing pages ────────────────────────────────────────────────────────

    /** The pages, without their {@code content}: a listing would otherwise carry every page's layout. */
    @NonNull CrmPage<CrmLandingPage> listLandingPages(@NonNull CrmSession session, @Nullable String cursor, int limit);

    @NonNull CrmLandingPage getLandingPage(@NonNull CrmSession session, @NonNull String id);

    /** Creates the page as a draft. */
    @NonNull String createLandingPage(@NonNull CrmSession session, @NonNull SaveCrmLandingPageBuilder page);

    void updateLandingPage(@NonNull CrmSession session, @NonNull String id, @NonNull SaveCrmLandingPageBuilder page);

    /** Publishes the page at {@code at}: scheduled when in the future, live otherwise. */
    void publishLandingPage(@NonNull CrmSession session, @NonNull String id, @NonNull Instant at);

    void deleteLandingPage(@NonNull CrmSession session, @NonNull String id);

    // ── forms ────────────────────────────────────────────────────────────────

    @NonNull CrmPage<CrmForm> listForms(@NonNull CrmSession session, @Nullable String cursor, int limit);

    @NonNull CrmForm getForm(@NonNull CrmSession session, @NonNull String id);

    @NonNull String createForm(@NonNull CrmSession session, @NonNull SaveCrmFormBuilder form);

    /** Writes the non-null parts; {@code fields}, when given, replaces every field of the form. */
    void updateForm(@NonNull CrmSession session, @NonNull String id, @NonNull SaveCrmFormBuilder form);

    void deleteForm(@NonNull CrmSession session, @NonNull String id);

    // ── bulk, for imports ────────────────────────────────────────────────────

    /** The contacts holding one of these emails; an email no contact holds is simply absent. */
    @NonNull List<CrmContact> findContactsByEmail(@NonNull CrmSession session, @NonNull Collection<String> emails);

    /** The contacts with exactly that first and last name. */
    @NonNull List<CrmContact> findContactsByName(@NonNull CrmSession session,
                                                 @NonNull String firstName,
                                                 @NonNull String lastName);

    /** The companies with one of these domains. */
    @NonNull List<CrmCompany> findCompaniesByDomain(@NonNull CrmSession session, @NonNull Collection<String> domains);

    /** The companies with exactly that name. */
    @NonNull List<CrmCompany> findCompaniesByName(@NonNull CrmSession session, @NonNull String name);

    @NonNull CrmBulkResult<CrmContact> createContacts(@NonNull CrmSession session,
                                                     @NonNull List<SaveCrmContactBuilder> contacts);

    /** @return the ids written, and the refused updates keyed by their index in {@code ids}. */
    @NonNull CrmBulkResult<String> updateContacts(@NonNull CrmSession session,
                                                  @NonNull List<String> ids,
                                                  @NonNull List<SaveCrmContactBuilder> contacts);

    @NonNull CrmBulkResult<CrmCompany> createCompanies(@NonNull CrmSession session,
                                                      @NonNull List<SaveCrmCompanyBuilder> companies);

    /** @return the ids written, and the refused updates keyed by their index in {@code ids}. */
    @NonNull CrmBulkResult<String> updateCompanies(@NonNull CrmSession session,
                                                   @NonNull List<String> ids,
                                                   @NonNull List<SaveCrmCompanyBuilder> companies);

    /** For each of {@code fromIds}, the ids of the {@code to} records it is associated with. */
    @NonNull Map<String, Set<String>> listAssociatedIds(@NonNull CrmSession session,
                                                        @NonNull CrmObjectType from,
                                                        @NonNull Collection<String> fromIds,
                                                        @NonNull CrmObjectType to);

    /**
     * Creates the default association of each link.
     *
     * @return the CRM's message for each refused batch or link — the CRM does not always tell which link it refused.
     */
    @NonNull List<String> associateAll(@NonNull CrmSession session,
                                       @NonNull CrmObjectType from,
                                       @NonNull CrmObjectType to,
                                       @NonNull List<CrmLink> links);

    /** Logs each note on its contact or company; {@code done} holds the ids of the notes created. */
    @NonNull CrmBulkResult<String> createNotes(@NonNull CrmSession session, @NonNull List<CrmNote> notes);
}
