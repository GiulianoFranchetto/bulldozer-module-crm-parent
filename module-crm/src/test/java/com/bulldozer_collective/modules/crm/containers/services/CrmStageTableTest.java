package com.bulldozer_collective.modules.crm.containers.services;

import com.bulldozer_collective.modules.crm.api.CrmContainerType;
import com.bulldozer_collective.modules.crm.api.CrmDealStage;
import com.bulldozer_collective.modules.crm.api.CrmLeadStage;
import com.bulldozer_collective.modules.crm.containers.data.CrmContainerEntity;
import com.bulldozer_collective.modules.crm.containers.data.CrmContainerStageEntity;
import com.bulldozer_collective.modules.crm.gateway.CrmStageWrite;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The stage table is where a customer's own stage values become ours and back, for one container. Nothing may be
 * assumed about the values: the source can be a custom property, and several values can mean the same stage.
 */
class CrmStageTableTest {

    @Test
    @DisplayName("a deal's stage is read from the container's source property, custom or not")
    void resolvesFromCustomProperty() {
        var table = CrmStageTable.deals(container(CrmContainerType.CRM_CONTAINER_TYPE_DEAL, "bdz_stage"),
                List.of(deal("signed", CrmDealStage.CRM_DEAL_STAGE_WON, false)));

        var value = table.value(Map.of("bdz_stage", "signed", "dealstage", "closedwon"));

        assertThat(value).isEqualTo("signed");
        assertThat(table.stage(value)).isEqualTo(CrmDealStage.CRM_DEAL_STAGE_WON);
    }

    @Test
    @DisplayName("an unmapped or unknown value has no stage")
    void unmappedValue() {
        var table = CrmStageTable.deals(container(CrmContainerType.CRM_CONTAINER_TYPE_DEAL, "dealstage"),
                List.of(deal("appointmentscheduled", null, false)));

        assertThat(table.stage("appointmentscheduled")).isNull();
        assertThat(table.stage("not-a-stage-of-this-container")).isNull();
        assertThat(table.stage(null)).isNull();
    }

    @Test
    @DisplayName("writing a stage mapped once writes that value")
    void writesSingleMapping() {
        var table = CrmStageTable.deals(container(CrmContainerType.CRM_CONTAINER_TYPE_DEAL, "bdz_stage"),
                List.of(deal("signed", CrmDealStage.CRM_DEAL_STAGE_WON, false)));

        assertThat(table.write(CrmDealStage.CRM_DEAL_STAGE_WON)).isEqualTo(new CrmStageWrite("bdz_stage", "signed"));
    }

    @Test
    @DisplayName("writing a stage mapped several times writes the write default")
    void writesDefaultAmongSeveral() {
        var table = CrmStageTable.deals(container(CrmContainerType.CRM_CONTAINER_TYPE_DEAL, "bdz_stage"),
                List.of(deal("signed", CrmDealStage.CRM_DEAL_STAGE_WON, false),
                        deal("paid", CrmDealStage.CRM_DEAL_STAGE_WON, true)));

        assertThat(table.write(CrmDealStage.CRM_DEAL_STAGE_WON).value()).isEqualTo("paid");
    }

    @Test
    @DisplayName("a stage mapped several times with no write default cannot be written")
    void refusesAmbiguousStage() {
        var table = CrmStageTable.deals(container(CrmContainerType.CRM_CONTAINER_TYPE_DEAL, "bdz_stage"),
                List.of(deal("signed", CrmDealStage.CRM_DEAL_STAGE_WON, false),
                        deal("paid", CrmDealStage.CRM_DEAL_STAGE_WON, false)));

        assertThatThrownBy(() -> table.write(CrmDealStage.CRM_DEAL_STAGE_WON))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("paid, signed");
    }

    @Test
    @DisplayName("an unmapped stage cannot be written")
    void refusesUnmappedStage() {
        var table = CrmStageTable.deals(container(CrmContainerType.CRM_CONTAINER_TYPE_DEAL, "dealstage"),
                List.of(deal("qualified", null, false)));

        assertThatThrownBy(() -> table.write(CrmDealStage.CRM_DEAL_STAGE_LOST))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not mapped");
    }

    @Test
    @DisplayName("a lead container reads and writes lead stages, never deal ones")
    void leadStages() {
        var stage = deal("attempting", CrmDealStage.CRM_DEAL_STAGE_WON, false);
        stage.setLeadStage(CrmLeadStage.CRM_LEAD_STAGE_ATTEMPTING);
        var table = CrmStageTable.leads(container(CrmContainerType.CRM_CONTAINER_TYPE_LEAD, "hs_pipeline_stage"),
                List.of(stage));

        assertThat(table.stage("attempting")).isEqualTo(CrmLeadStage.CRM_LEAD_STAGE_ATTEMPTING);
        assertThat(table.write(CrmLeadStage.CRM_LEAD_STAGE_ATTEMPTING))
                .isEqualTo(new CrmStageWrite("hs_pipeline_stage", "attempting"));
    }

    private static CrmContainerEntity container(CrmContainerType type, String stagePropertyName) {
        var container = new CrmContainerEntity();
        container.setType(type);
        container.setRemoteId("pipeline-1");
        container.setStagePropertyName(stagePropertyName);
        return container;
    }

    private static CrmContainerStageEntity deal(String value, CrmDealStage stage, boolean writeDefault) {
        var entity = new CrmContainerStageEntity();
        entity.setRemoteId(value);
        entity.setName(value);
        entity.setDealStage(stage);
        entity.setWriteDefault(writeDefault);
        return entity;
    }
}
