package com.bulldozer_collective.modules.crm.gateway.salesforce;

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
import com.bulldozer_collective.modules.crm.api.CrmRemoteContainer;
import com.bulldozer_collective.modules.crm.companies.services.SaveCrmCompanyBuilder;
import com.bulldozer_collective.modules.crm.contacts.services.SaveCrmContactBuilder;
import com.bulldozer_collective.modules.crm.deals.services.SaveCrmDealBuilder;
import com.bulldozer_collective.modules.crm.gateway.CrmContainedRecord;
import com.bulldozer_collective.modules.crm.gateway.CrmGateway;
import com.bulldozer_collective.modules.crm.gateway.CrmSession;
import com.bulldozer_collective.modules.crm.gateway.CrmStageOption;
import com.bulldozer_collective.modules.crm.gateway.CrmStageWrite;
import com.bulldozer_collective.modules.crm.gateway.ListCrmObjectsBuilder;
import com.bulldozer_collective.modules.crm.leads.services.CreateCrmLeadBuilder;
import com.bulldozer_collective.shared.IntegrationPlatform;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Salesforce is not implemented yet: the modulith has no Salesforce OAuth flow. Every call answers {@code 501}.
 * When implementing it: a container is a business process (a sales process for opportunities, a lead process
 * for leads), reached through {@code RecordType.BusinessProcessId}; read the instance URL from
 * {@link CrmSession#instanceUrl()}; check the REST API reference rather than guessing a path.
 */
@Component
public class SalesforceCrmGateway implements CrmGateway {

    @Override
    public @NonNull IntegrationPlatform platform() {
        return IntegrationPlatform.INTEGRATION_PLATFORM_SALESFORCE;
    }

    @Override
    public @NonNull CrmPage<CrmContact> listContacts(@NonNull CrmSession session,
                                                     @NonNull ListCrmObjectsBuilder query) {
        throw notSupported();
    }

    @Override
    public @NonNull CrmContact getContact(@NonNull CrmSession session,
                                          @NonNull String id) {
        throw notSupported();
    }

    @Override
    public @NonNull String createContact(@NonNull CrmSession session,
                                         @NonNull SaveCrmContactBuilder contact) {
        throw notSupported();
    }

    @Override
    public void updateContact(@NonNull CrmSession session,
                              @NonNull String id,
                              @NonNull SaveCrmContactBuilder contact) {
        throw notSupported();
    }

    @Override
    public @NonNull CrmPage<CrmCompany> listCompanies(@NonNull CrmSession session,
                                                      @NonNull ListCrmObjectsBuilder query) {
        throw notSupported();
    }

    @Override
    public @NonNull CrmCompany getCompany(@NonNull CrmSession session,
                                          @NonNull String id) {
        throw notSupported();
    }

    @Override
    public @NonNull String createCompany(@NonNull CrmSession session,
                                         @NonNull SaveCrmCompanyBuilder company) {
        throw notSupported();
    }

    @Override
    public void updateCompany(@NonNull CrmSession session,
                              @NonNull String id,
                              @NonNull SaveCrmCompanyBuilder company) {
        throw notSupported();
    }

    @Override
    public @NonNull List<CrmRemoteContainer> listRemoteContainers(@NonNull CrmSession session,
                                                                  @NonNull CrmContainerType type) {
        throw notSupported();
    }

    @Override
    public @NonNull List<CrmProperty> listProperties(@NonNull CrmSession session,
                                                     @NonNull CrmContainerType type) {
        throw notSupported();
    }

    @Override
    public @NonNull List<CrmStageOption> listStageOptions(@NonNull CrmSession session,
                                                          @NonNull CrmContainerType type,
                                                          @NonNull String containerRemoteId,
                                                          @NonNull String propertyName) {
        throw notSupported();
    }

    @Override
    public @NonNull CrmPage<CrmContainedRecord<CrmDeal>> listDeals(@NonNull CrmSession session,
                                                                   @NonNull String containerRemoteId,
                                                                   @NonNull ListCrmObjectsBuilder query,
                                                                   @NonNull String stageProperty) {
        throw notSupported();
    }

    @Override
    public @NonNull CrmContainedRecord<CrmDeal> getDeal(@NonNull CrmSession session,
                                                        @NonNull String id,
                                                        @NonNull String stageProperty) {
        throw notSupported();
    }

    @Override
    public @NonNull String createDeal(@NonNull CrmSession session,
                                      @NonNull String containerRemoteId,
                                      @NonNull SaveCrmDealBuilder deal,
                                      @NonNull CrmStageWrite stage) {
        throw notSupported();
    }

    @Override
    public void updateDeal(@NonNull CrmSession session,
                           @NonNull String id,
                           @NonNull SaveCrmDealBuilder deal,
                           @Nullable CrmStageWrite stage) {
        throw notSupported();
    }

    @Override
    public @NonNull CrmPage<CrmContainedRecord<CrmLead>> listLeads(@NonNull CrmSession session,
                                                                   @NonNull String containerRemoteId,
                                                                   @NonNull ListCrmObjectsBuilder query,
                                                                   @NonNull String stageProperty) {
        throw notSupported();
    }

    @Override
    public @NonNull CrmContainedRecord<CrmLead> getLead(@NonNull CrmSession session,
                                                        @NonNull String id,
                                                        @NonNull String stageProperty) {
        throw notSupported();
    }

    @Override
    public @NonNull String createLead(@NonNull CrmSession session,
                                      @NonNull String containerRemoteId,
                                      @NonNull CreateCrmLeadBuilder lead,
                                      @NonNull CrmStageWrite stage) {
        throw notSupported();
    }

    @Override
    public @NonNull CrmPage<CrmActivity> listActivities(@NonNull CrmSession session,
                                                        @NonNull CrmActivityType type,
                                                        @NonNull ListCrmObjectsBuilder query) {
        throw notSupported();
    }

    @Override
    public @NonNull CrmActivity getActivity(@NonNull CrmSession session,
                                            @NonNull CrmActivityType type,
                                            @NonNull String id) {
        throw notSupported();
    }

    @Override
    public @NonNull String createActivity(@NonNull CrmSession session,
                                          @NonNull CreateCrmActivityBuilder activity) {
        throw notSupported();
    }

    @Override
    public void associate(@NonNull CrmSession session,
                          @NonNull CrmObjectRef from,
                          @NonNull CrmObjectRef to) {
        throw notSupported();
    }

    @Override
    public void dissociate(@NonNull CrmSession session,
                           @NonNull CrmObjectRef from,
                           @NonNull CrmObjectRef to) {
        throw notSupported();
    }

    private static UnsupportedOperationException notSupported() {
        return new UnsupportedOperationException("Salesforce is not supported yet");
    }
}
