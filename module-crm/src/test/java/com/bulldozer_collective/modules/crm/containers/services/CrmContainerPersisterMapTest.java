package com.bulldozer_collective.modules.crm.containers.services;

import com.bulldozer_collective.modules.crm.api.CrmDealStage;
import com.bulldozer_collective.modules.crm.containers.data.CrmContainerRepository;
import com.bulldozer_collective.modules.crm.containers.data.CrmContainerStageEntity;
import com.bulldozer_collective.modules.crm.containers.data.CrmContainerStageRepository;
import com.bulldozer_collective.logging.BulldozerLogger;
import com.bulldozer_collective.shared.exceptions.NotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CrmContainerPersisterMapTest {

    private static final UUID CUSTOMER = UUID.randomUUID();
    private static final UUID PROJECT = UUID.randomUUID();
    private static final UUID CONTAINER = UUID.randomUUID();

    private final CrmContainerStageRepository stageRepository = mock(CrmContainerStageRepository.class);
    private final CrmContainerPersister persister = new CrmContainerPersister(mock(CrmContainerRepository.class),
            this.stageRepository, mock(BulldozerLogger.class));

    @Test
    @DisplayName("a write default given in the batch takes the mark off a stage left out of it")
    void writeDefaultMovesOffUnlistedStage() {
        var signed = stage("signed", CrmDealStage.CRM_DEAL_STAGE_WON, true);
        var paid = stage("paid", null, false);
        this.given(signed, paid);

        this.persister.map(CUSTOMER, PROJECT, CONTAINER, List.of(MapCrmContainerStageBuilder.builder()
                .stageId(paid.getId()).dealStage(CrmDealStage.CRM_DEAL_STAGE_WON).writeDefault(true).build()));

        assertThat(paid.isWriteDefault()).isTrue();
        assertThat(signed.isWriteDefault()).isFalse();
        assertThat(signed.getDealStage()).isEqualTo(CrmDealStage.CRM_DEAL_STAGE_WON);
    }

    @Test
    @DisplayName("unmapping a stage clears its write default")
    void unmappedIsNeverDefault() {
        var signed = stage("signed", CrmDealStage.CRM_DEAL_STAGE_WON, true);
        this.given(signed);

        this.persister.map(CUSTOMER, PROJECT, CONTAINER, List.of(MapCrmContainerStageBuilder.builder()
                .stageId(signed.getId()).writeDefault(true).build()));

        assertThat(signed.getDealStage()).isNull();
        assertThat(signed.isWriteDefault()).isFalse();
    }

    @Test
    @DisplayName("a stage of another container fails the whole batch before anything is saved")
    void unknownStage() {
        this.given(stage("signed", null, false));

        assertThatThrownBy(() -> this.persister.map(CUSTOMER, PROJECT, CONTAINER, List.of(
                MapCrmContainerStageBuilder.builder().stageId(UUID.randomUUID())
                        .dealStage(CrmDealStage.CRM_DEAL_STAGE_WON).build())))
                .isInstanceOf(NotFoundException.class);
        verify(this.stageRepository, never()).saveAll(any());
    }

    private void given(CrmContainerStageEntity... stages) {
        when(this.stageRepository.findAllByCustomerIdAndProjectIdAndContainer_IdOrderByDisplayOrderAsc(
                CUSTOMER, PROJECT, CONTAINER)).thenReturn(new ArrayList<>(List.of(stages)));
        when(this.stageRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private static CrmContainerStageEntity stage(String value, CrmDealStage stage, boolean writeDefault) {
        var entity = new CrmContainerStageEntity();
        entity.setId(UUID.randomUUID());
        entity.setRemoteId(value);
        entity.setName(value);
        entity.setDealStage(stage);
        entity.setWriteDefault(writeDefault);
        return entity;
    }
}
