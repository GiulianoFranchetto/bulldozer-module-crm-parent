package com.bulldozer_collective.modules.crm.containers.services;

import com.bulldozer_collective.modules.crm.api.CrmContainerType;
import com.bulldozer_collective.modules.crm.connections.data.CrmConnectionEntity;
import com.bulldozer_collective.modules.crm.connections.services.CrmConnectionFetcher;
import com.bulldozer_collective.modules.crm.containers.data.CrmContainerEntity;
import com.bulldozer_collective.modules.crm.containers.data.CrmContainerRepository;
import com.bulldozer_collective.modules.crm.containers.data.CrmContainerStageEntity;
import com.bulldozer_collective.modules.crm.containers.data.CrmContainerStageRepository;
import com.bulldozer_collective.modules.crm.gateway.CrmGateway;
import com.bulldozer_collective.modules.crm.gateway.CrmGatewayResolver;
import com.bulldozer_collective.modules.crm.gateway.CrmRemoteContainer;
import com.bulldozer_collective.modules.crm.gateway.CrmRemoteContainerStage;
import com.bulldozer_collective.modules.crm.gateway.CrmSession;
import com.bulldozer_collective.modules.crm.gateway.CrmSessionOpener;
import com.bulldozer_collective.modules.crm.gateway.CrmStageOption;
import com.bulldozer_collective.logging.BulldozerLogger;
import com.bulldozer_collective.shared.IntegrationPlatform;
import com.bulldozer_collective.shared.exceptions.ConflictException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CrmContainerFactoryTest {

    private static final UUID CUSTOMER = UUID.randomUUID();
    private static final UUID PROJECT = UUID.randomUUID();
    private static final UUID CONNECTION = UUID.randomUUID();
    private static final CrmContainerType DEAL = CrmContainerType.CRM_CONTAINER_TYPE_DEAL;

    private final CrmConnectionFetcher connectionFetcher = mock(CrmConnectionFetcher.class);
    private final CrmSessionOpener sessionOpener = mock(CrmSessionOpener.class);
    private final CrmGatewayResolver gatewayResolver = mock(CrmGatewayResolver.class);
    private final CrmGateway gateway = mock(CrmGateway.class);
    private final CrmContainerRepository containerRepository = mock(CrmContainerRepository.class);
    private final CrmContainerStageRepository stageRepository = mock(CrmContainerStageRepository.class);
    private final CrmContainerFactory factory = new CrmContainerFactory(this.connectionFetcher, this.sessionOpener,
            this.gatewayResolver, new CrmStageSources(),
            new CrmContainerPersister(this.containerRepository, this.stageRepository, mock(BulldozerLogger.class)));

    @Test
    @DisplayName("each container gets its own stages, in one saveAll per table")
    void importsInBatch() throws Exception {
        this.given();
        when(this.containerRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        var created = this.factory.create(CUSTOMER, PROJECT, CONNECTION, List.of(
                ImportCrmContainerBuilder.builder().type(DEAL).remoteId("sales").build(),
                ImportCrmContainerBuilder.builder().type(DEAL).remoteId("renewals").build()));

        assertThat(created).extracting(CrmContainerEntity::getRemoteId).containsExactly("sales", "renewals");
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CrmContainerStageEntity>> stages = ArgumentCaptor.forClass(List.class);
        verify(this.stageRepository).saveAll(stages.capture());
        assertThat(stages.getValue())
                .extracting(stage -> stage.getContainer().getRemoteId() + "/" + stage.getRemoteId())
                .containsExactly("sales/sales-1", "renewals/renewals-1");
    }

    @Test
    @DisplayName("a concurrent import of the same container answers 409, not 500")
    void concurrentImportConflicts() throws Exception {
        this.given();
        when(this.containerRepository.saveAll(anyList())).thenThrow(new DataIntegrityViolationException("uq"));

        assertThatThrownBy(() -> this.factory.create(CUSTOMER, PROJECT, CONNECTION,
                List.of(ImportCrmContainerBuilder.builder().type(DEAL).remoteId("sales").build())))
                .isInstanceOf(ConflictException.class);
    }

    private void given() throws Exception {
        var session = CrmSession.builder().customerId(CUSTOMER).projectId(PROJECT).connectionId(CONNECTION)
                .platform(IntegrationPlatform.INTEGRATION_PLATFORM_HUBSPOT).accessToken("token").build();
        var connection = new CrmConnectionEntity();
        connection.setId(CONNECTION);
        connection.setCustomerId(CUSTOMER);
        connection.setProjectId(PROJECT);
        when(this.connectionFetcher.fetch(CUSTOMER, PROJECT, CONNECTION)).thenReturn(connection);
        when(this.sessionOpener.open(CUSTOMER, PROJECT, CONNECTION)).thenReturn(session);
        when(this.gatewayResolver.resolve(session)).thenReturn(this.gateway);
        when(this.gateway.nativeStageProperty(DEAL)).thenReturn("dealstage");
        when(this.gateway.listRemoteContainers(session, DEAL)).thenReturn(List.of(remote("sales"), remote("renewals")));
        when(this.gateway.listStageOptions(any(), any(), any(), any())).thenAnswer(invocation -> List.of(
                new CrmStageOption(invocation.getArgument(2) + "-1", "Stage", 0)));
    }

    private static CrmRemoteContainer remote(String remoteId) {
        return CrmRemoteContainer.builder().remoteId(remoteId).name(remoteId)
                .stages(List.of(CrmRemoteContainerStage.builder().remoteId(remoteId + "-1").name("Stage").build()))
                .build();
    }
}
