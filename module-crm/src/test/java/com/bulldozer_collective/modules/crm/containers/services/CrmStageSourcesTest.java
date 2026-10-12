package com.bulldozer_collective.modules.crm.containers.services;

import com.bulldozer_collective.modules.crm.api.CrmContainerType;
import com.bulldozer_collective.modules.crm.containers.data.CrmContainerEntity;
import com.bulldozer_collective.modules.crm.gateway.CrmGateway;
import com.bulldozer_collective.modules.crm.gateway.CrmSession;
import com.bulldozer_collective.modules.crm.gateway.CrmStageOption;
import com.bulldozer_collective.modules.crm.gateway.CrmStageWrite;
import com.bulldozer_collective.shared.IntegrationPlatform;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The CRM files every deal and lead under one of its pipeline's own stages. A container reading its stages from a
 * custom property cannot designate that stage, so the caller must: these tests pin when it is asked for.
 */
class CrmStageSourcesTest {

    private static final CrmStageWrite MAPPED = new CrmStageWrite("bdz_stage", "signed");

    private final CrmGateway gateway = mock(CrmGateway.class);
    private final CrmStageSources stageSources = new CrmStageSources();
    private final CrmSession session = CrmSession.builder()
            .customerId(UUID.randomUUID()).projectId(UUID.randomUUID()).connectionId(UUID.randomUUID())
            .platform(IntegrationPlatform.INTEGRATION_PLATFORM_HUBSPOT).accessToken("token").build();

    @Test
    @DisplayName("a native stage source writes the mapped value alone, and refuses a pipelineStage")
    void nativeSource() {
        var container = container("dealstage");

        assertThat(this.stageSources.creationWrites(this.session, this.gateway, container, MAPPED, null))
                .containsExactly(MAPPED);
        assertThatThrownBy(() -> this.stageSources.creationWrites(this.session, this.gateway, container, MAPPED, "1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("pipelineStage:");
        verify(this.gateway, never()).listStageOptions(any(), any(), any(), any());
    }

    @Test
    @DisplayName("a custom stage source requires a pipelineStage of the pipeline, written to the native property")
    void customSource() {
        var container = container("bdz_stage");
        when(this.gateway.listStageOptions(this.session, CrmContainerType.CRM_CONTAINER_TYPE_DEAL, "sales",
                "dealstage")).thenReturn(List.of(new CrmStageOption("1", "Appointment", 0)));

        assertThat(this.stageSources.creationWrites(this.session, this.gateway, container, MAPPED, "1"))
                .containsExactly(MAPPED, new CrmStageWrite("dealstage", "1"));
        assertThatThrownBy(() -> this.stageSources.creationWrites(this.session, this.gateway, container, MAPPED, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("pipelineStage: required");
        assertThatThrownBy(() -> this.stageSources.creationWrites(this.session, this.gateway, container, MAPPED, "9"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("is not a stage of this pipeline");
    }

    private CrmContainerEntity container(String stagePropertyName) {
        when(this.gateway.nativeStageProperty(CrmContainerType.CRM_CONTAINER_TYPE_DEAL)).thenReturn("dealstage");
        var container = new CrmContainerEntity();
        container.setId(UUID.randomUUID());
        container.setType(CrmContainerType.CRM_CONTAINER_TYPE_DEAL);
        container.setRemoteId("sales");
        container.setStagePropertyName(stagePropertyName);
        return container;
    }
}
