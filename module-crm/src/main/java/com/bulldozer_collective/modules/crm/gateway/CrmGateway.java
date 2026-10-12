package com.bulldozer_collective.modules.crm.gateway;

import com.bulldozer_collective.modules.crm.activities.services.CreateCrmActivityBuilder;
import com.bulldozer_collective.modules.crm.api.CrmActivity;
import com.bulldozer_collective.modules.crm.api.CrmActivityType;
import com.bulldozer_collective.modules.crm.api.CrmCompany;
import com.bulldozer_collective.modules.crm.api.CrmContact;
import com.bulldozer_collective.modules.crm.api.CrmContainerType;
import com.bulldozer_collective.modules.crm.api.CrmDeal;
import com.bulldozer_collective.modules.crm.api.CrmLead;
import com.bulldozer_collective.modules.crm.api.CrmObjectRef;
import com.bulldozer_collective.modules.crm.api.CrmPage;
import com.bulldozer_collective.modules.crm.api.CrmProperty;
import com.bulldozer_collective.modules.crm.companies.services.SaveCrmCompanyBuilder;
import com.bulldozer_collective.modules.crm.contacts.services.SaveCrmContactBuilder;
import com.bulldozer_collective.modules.crm.deals.services.SaveCrmDealBuilder;
import com.bulldozer_collective.modules.crm.leads.services.CreateCrmLeadBuilder;
import com.bulldozer_collective.shared.IntegrationPlatform;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

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

    /** The properties of the CRM's deals or leads, candidates for a stage source. */
    @NonNull List<CrmProperty> listProperties(@NonNull CrmSession session, @NonNull CrmContainerType type);

    /**
     * The values {@code propertyName} can take in the container — empty when the property is free-form.
     *
     * @throws IllegalArgumentException when the CRM has no such property.
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

    @NonNull String createDeal(@NonNull CrmSession session,
                               @NonNull String containerRemoteId,
                               @NonNull SaveCrmDealBuilder deal,
                               @NonNull CrmStageWrite stage);

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

    @NonNull String createLead(@NonNull CrmSession session,
                               @NonNull String containerRemoteId,
                               @NonNull CreateCrmLeadBuilder lead,
                               @NonNull CrmStageWrite stage);

    // ── activities ───────────────────────────────────────────────────────────

    @NonNull CrmPage<CrmActivity> listActivities(@NonNull CrmSession session,
                                                 @NonNull CrmActivityType type,
                                                 @NonNull ListCrmObjectsBuilder query);

    @NonNull CrmActivity getActivity(@NonNull CrmSession session, @NonNull CrmActivityType type, @NonNull String id);

    @NonNull String createActivity(@NonNull CrmSession session, @NonNull CreateCrmActivityBuilder activity);

    // ── associations ─────────────────────────────────────────────────────────

    void associate(@NonNull CrmSession session, @NonNull CrmObjectRef from, @NonNull CrmObjectRef to);

    void dissociate(@NonNull CrmSession session, @NonNull CrmObjectRef from, @NonNull CrmObjectRef to);
}
