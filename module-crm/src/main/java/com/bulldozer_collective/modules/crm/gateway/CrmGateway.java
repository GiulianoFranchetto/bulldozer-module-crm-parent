package com.bulldozer_collective.modules.crm.gateway;

import com.bulldozer_collective.modules.crm.activities.services.CreateCrmActivityBuilder;
import com.bulldozer_collective.modules.crm.api.CrmActivity;
import com.bulldozer_collective.modules.crm.api.CrmActivityType;
import com.bulldozer_collective.modules.crm.api.CrmCompany;
import com.bulldozer_collective.modules.crm.api.CrmContact;
import com.bulldozer_collective.modules.crm.api.CrmDealProperty;
import com.bulldozer_collective.modules.crm.api.CrmObjectRef;
import com.bulldozer_collective.modules.crm.api.CrmPage;
import com.bulldozer_collective.modules.crm.api.CrmPipeline;
import com.bulldozer_collective.modules.crm.companies.services.SaveCrmCompanyBuilder;
import com.bulldozer_collective.modules.crm.contacts.services.SaveCrmContactBuilder;
import com.bulldozer_collective.modules.crm.deals.services.SaveCrmDealBuilder;
import com.bulldozer_collective.shared.IntegrationPlatform;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * Façade over one CRM. Each implementation translates between the module's own model ({@code crm-api}) and the
 * CRM's API, and stores nothing: every call goes to the CRM.
 *
 * <p>Ids are the CRM's own. Writes return the id of the record, never the record: the services read it back, so a
 * write answers with what the CRM actually holds. Implementations translate the CRM's errors: a missing record
 * is a {@code NotFoundException}, a rejected write a {@code BadRequestException} or a {@code ConflictException}
 * carrying the CRM's message, a refused token a {@link CrmAuthorizationException}.
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

    // ── deals ────────────────────────────────────────────────────────────────

    /** @param stageProperties extra properties to read on every deal: the stage sources of the project. */
    @NonNull CrmPage<CrmDealRecord> listDeals(@NonNull CrmSession session,
                                              @NonNull ListCrmObjectsBuilder query,
                                              @NonNull Collection<String> stageProperties);

    @NonNull CrmDealRecord getDeal(@NonNull CrmSession session,
                                   @NonNull String id,
                                   @NonNull Collection<String> stageProperties);

    @NonNull String createDeal(@NonNull CrmSession session,
                               @NonNull SaveCrmDealBuilder deal,
                               @NonNull CrmStageWrite stage);

    void updateDeal(@NonNull CrmSession session,
                    @NonNull String id,
                    @NonNull SaveCrmDealBuilder deal,
                    @Nullable CrmStageWrite stage);

    @NonNull List<CrmPipeline> listPipelines(@NonNull CrmSession session);

    @NonNull List<CrmDealProperty> listDealProperties(@NonNull CrmSession session);

    /**
     * The values a deal of {@code pipelineId} may take in {@code propertyName}, or an empty list when the property
     * is free-form and any value is accepted.
     */
    @NonNull List<String> listStageValues(@NonNull CrmSession session,
                                          @NonNull String pipelineId,
                                          @NonNull String propertyName);

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
