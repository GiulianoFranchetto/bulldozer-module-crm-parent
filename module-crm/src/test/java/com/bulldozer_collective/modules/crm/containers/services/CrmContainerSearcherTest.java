package com.bulldozer_collective.modules.crm.containers.services;

import com.bulldozer_collective.modules.crm.api.CrmContainerEntry;
import com.bulldozer_collective.modules.crm.api.CrmContainerType;
import com.bulldozer_collective.modules.crm.containers.data.CrmContainerEntity;
import com.bulldozer_collective.modules.crm.gateway.CrmGateway;
import com.bulldozer_collective.modules.crm.gateway.CrmGatewayResolver;
import com.bulldozer_collective.modules.crm.gateway.CrmRemoteContainer;
import com.bulldozer_collective.modules.crm.gateway.CrmSession;
import com.bulldozer_collective.modules.crm.gateway.CrmSessionOpener;
import com.bulldozer_collective.shared.IntegrationPlatform;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** One listing answers both "what does the CRM have" and "what did the project import". */
class CrmContainerSearcherTest {

    private static final UUID CUSTOMER = UUID.randomUUID();
    private static final UUID PROJECT = UUID.randomUUID();
    private static final UUID CONNECTION = UUID.randomUUID();

    private final CrmContainerFetcher fetcher = mock(CrmContainerFetcher.class);
    private final CrmSessionOpener sessionOpener = mock(CrmSessionOpener.class);
    private final CrmGatewayResolver gatewayResolver = mock(CrmGatewayResolver.class);
    private final CrmGateway gateway = mock(CrmGateway.class);
    private final CrmContainerSearcher searcher =
            new CrmContainerSearcher(this.fetcher, this.sessionOpener, this.gatewayResolver);

    @Test
    @DisplayName("CRM containers come flagged selected when imported; an imported one gone from the CRM comes last")
    void mergesImportedAndRemote() throws Exception {
        var sales = imported(CrmContainerType.CRM_CONTAINER_TYPE_DEAL, "sales");
        var gone = imported(CrmContainerType.CRM_CONTAINER_TYPE_DEAL, "old");
        this.given(List.of(sales, gone), CrmContainerType.CRM_CONTAINER_TYPE_DEAL, List.of(remote("sales"), remote("renewals")));

        var entries = this.searcher.list(CUSTOMER, PROJECT, CONNECTION, CrmContainerType.CRM_CONTAINER_TYPE_DEAL);

        assertThat(entries).extracting(CrmContainerEntry::remoteId).containsExactly("sales", "renewals", "old");
        assertThat(entries.get(0).selected()).isTrue();
        assertThat(entries.get(0).containerId()).isEqualTo(sales.getId());
        assertThat(entries.get(1).selected()).isFalse();
        assertThat(entries.get(1).containerId()).isNull();
        assertThat(entries.get(2).existsInCrm()).isFalse();
        verify(this.gateway, never()).listRemoteContainers(any(), eq(CrmContainerType.CRM_CONTAINER_TYPE_LEAD));
    }

    @Test
    @DisplayName("a deal container and a lead container sharing a remote id are told apart by their type")
    void matchesOnTypeToo() throws Exception {
        var lead = imported(CrmContainerType.CRM_CONTAINER_TYPE_LEAD, "default");
        this.given(List.of(lead), CrmContainerType.CRM_CONTAINER_TYPE_DEAL, List.of(remote("default")));

        var entries = this.searcher.list(CUSTOMER, PROJECT, CONNECTION, CrmContainerType.CRM_CONTAINER_TYPE_DEAL);

        assertThat(entries.get(0).selected()).isFalse();
    }

    private void given(List<CrmContainerEntity> imported, CrmContainerType type, List<CrmRemoteContainer> remote)
            throws Exception {
        var session = CrmSession.builder().customerId(CUSTOMER).projectId(PROJECT).connectionId(CONNECTION)
                .platform(IntegrationPlatform.INTEGRATION_PLATFORM_HUBSPOT).accessToken("token").build();
        when(this.fetcher.fetchAll(CUSTOMER, PROJECT, CONNECTION, type)).thenReturn(imported);
        when(this.sessionOpener.open(CUSTOMER, PROJECT, CONNECTION)).thenReturn(session);
        when(this.gatewayResolver.resolve(session)).thenReturn(this.gateway);
        when(this.gateway.listRemoteContainers(session, type)).thenReturn(remote);
    }

    private static CrmContainerEntity imported(CrmContainerType type, String remoteId) {
        var container = new CrmContainerEntity();
        container.setId(UUID.randomUUID());
        container.setType(type);
        container.setRemoteId(remoteId);
        container.setName(remoteId);
        return container;
    }

    private static CrmRemoteContainer remote(String remoteId) {
        return CrmRemoteContainer.builder().remoteId(remoteId).name(remoteId).stages(List.of()).build();
    }
}
