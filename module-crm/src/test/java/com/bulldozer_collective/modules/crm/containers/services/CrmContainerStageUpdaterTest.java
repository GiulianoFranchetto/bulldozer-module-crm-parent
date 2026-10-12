package com.bulldozer_collective.modules.crm.containers.services;

import com.bulldozer_collective.modules.crm.api.CrmContainerType;
import com.bulldozer_collective.modules.crm.api.CrmDealStage;
import com.bulldozer_collective.modules.crm.api.CrmLeadStage;
import com.bulldozer_collective.modules.crm.containers.data.CrmContainerEntity;
import com.bulldozer_collective.modules.crm.containers.data.CrmContainerStageRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** A lead stage in a deal container (or the reverse) would never be read back: it is refused up front. */
class CrmContainerStageUpdaterTest {

    private static final UUID CUSTOMER = UUID.randomUUID();
    private static final UUID PROJECT = UUID.randomUUID();
    private static final UUID CONNECTION = UUID.randomUUID();
    private static final UUID CONTAINER = UUID.randomUUID();

    private final CrmContainerFetcher fetcher = mock(CrmContainerFetcher.class);
    private final CrmContainerStageRepository repository = mock(CrmContainerStageRepository.class);
    private final CrmContainerPersister persister = mock(CrmContainerPersister.class);
    private final CrmContainerStageUpdater updater = new CrmContainerStageUpdater(this.fetcher, this.repository,
            this.persister);

    @Test
    @DisplayName("a lead stage is refused in a deal container")
    void leadStageInDealContainer() {
        this.givenContainer(CrmContainerType.CRM_CONTAINER_TYPE_DEAL);

        assertThatThrownBy(() -> this.updater.map(CUSTOMER, PROJECT, CONNECTION, CONTAINER, UUID.randomUUID(),
                MapCrmContainerStageBuilder.builder().leadStage(CrmLeadStage.CRM_LEAD_STAGE_NEW).build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dealStage");
        verifyNoInteractions(this.persister);
    }

    @Test
    @DisplayName("a deal stage is refused in a lead container")
    void dealStageInLeadContainer() {
        this.givenContainer(CrmContainerType.CRM_CONTAINER_TYPE_LEAD);

        assertThatThrownBy(() -> this.updater.map(CUSTOMER, PROJECT, CONNECTION, CONTAINER, UUID.randomUUID(),
                MapCrmContainerStageBuilder.builder().dealStage(CrmDealStage.CRM_DEAL_STAGE_WON).build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("leadStage");
        verifyNoInteractions(this.persister);
    }

    private void givenContainer(CrmContainerType type) {
        var container = new CrmContainerEntity();
        container.setType(type);
        when(this.fetcher.fetch(any(), any(), any(), any())).thenReturn(container);
    }
}
