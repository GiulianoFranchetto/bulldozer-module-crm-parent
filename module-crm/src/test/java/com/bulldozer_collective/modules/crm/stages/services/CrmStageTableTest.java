package com.bulldozer_collective.modules.crm.stages.services;

import com.bulldozer_collective.modules.crm.api.CrmDeal;
import com.bulldozer_collective.modules.crm.api.CrmDealStage;
import com.bulldozer_collective.modules.crm.gateway.CrmDealRecord;
import com.bulldozer_collective.modules.crm.gateway.CrmStageWrite;
import com.bulldozer_collective.modules.crm.stages.data.CrmStageMappingEntity;
import com.bulldozer_collective.modules.crm.stages.data.CrmStageSourceEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The stage table is where a customer's own stage values become ours and back. Nothing may be assumed about the
 * values themselves: the source can be a custom property, and several values can mean the same stage.
 */
class CrmStageTableTest {

    private static final String PIPELINE = "sales";

    @Test
    @DisplayName("a deal reads its stage from its pipeline's source property, custom or not")
    void resolvesFromCustomProperty() {
        var table = new CrmStageTable(
                List.of(source(PIPELINE, "bdz_stage")),
                List.of(mapping(PIPELINE, "signed", CrmDealStage.CRM_DEAL_STAGE_WON, false)));

        var deal = table.resolve(record(PIPELINE, Map.of("bdz_stage", "signed", "dealstage", "closedwon")));

        assertThat(deal.stage()).isEqualTo(CrmDealStage.CRM_DEAL_STAGE_WON);
        assertThat(deal.stageValue()).isEqualTo("signed");
    }

    @Test
    @DisplayName("an unmapped value keeps its raw value and no stage")
    void unmappedValue() {
        var table = new CrmStageTable(List.of(source(PIPELINE, "dealstage")), List.of());

        var deal = table.resolve(record(PIPELINE, Map.of("dealstage", "appointmentscheduled")));

        assertThat(deal.stage()).isNull();
        assertThat(deal.stageValue()).isEqualTo("appointmentscheduled");
    }

    @Test
    @DisplayName("a pipeline with no stage source leaves both stage fields empty")
    void noSource() {
        var table = new CrmStageTable(List.of(), List.of());

        var deal = table.resolve(record(PIPELINE, Map.of("dealstage", "closedwon")));

        assertThat(deal.stage()).isNull();
        assertThat(deal.stageValue()).isNull();
    }

    @Test
    @DisplayName("a mapping of another pipeline never applies")
    void mappingsArePerPipeline() {
        var table = new CrmStageTable(
                List.of(source(PIPELINE, "dealstage"), source("renewals", "dealstage")),
                List.of(mapping("renewals", "42", CrmDealStage.CRM_DEAL_STAGE_WON, false)));

        assertThat(table.resolve(record(PIPELINE, Map.of("dealstage", "42"))).stage()).isNull();
        assertThat(table.write("renewals", CrmDealStage.CRM_DEAL_STAGE_WON))
                .isEqualTo(new CrmStageWrite("dealstage", "42"));
    }

    @Test
    @DisplayName("writing a stage mapped once writes that value")
    void writesSingleMapping() {
        var table = new CrmStageTable(
                List.of(source(PIPELINE, "bdz_stage")),
                List.of(mapping(PIPELINE, "signed", CrmDealStage.CRM_DEAL_STAGE_WON, false)));

        assertThat(table.write(PIPELINE, CrmDealStage.CRM_DEAL_STAGE_WON))
                .isEqualTo(new CrmStageWrite("bdz_stage", "signed"));
    }

    @Test
    @DisplayName("writing a stage mapped several times writes the write default")
    void writesDefaultAmongSeveral() {
        var table = new CrmStageTable(
                List.of(source(PIPELINE, "bdz_stage")),
                List.of(mapping(PIPELINE, "signed", CrmDealStage.CRM_DEAL_STAGE_WON, false),
                        mapping(PIPELINE, "paid", CrmDealStage.CRM_DEAL_STAGE_WON, true)));

        assertThat(table.write(PIPELINE, CrmDealStage.CRM_DEAL_STAGE_WON).value()).isEqualTo("paid");
    }

    @Test
    @DisplayName("a stage mapped several times with no write default cannot be written")
    void refusesAmbiguousStage() {
        var table = new CrmStageTable(
                List.of(source(PIPELINE, "bdz_stage")),
                List.of(mapping(PIPELINE, "signed", CrmDealStage.CRM_DEAL_STAGE_WON, false),
                        mapping(PIPELINE, "paid", CrmDealStage.CRM_DEAL_STAGE_WON, false)));

        assertThatThrownBy(() -> table.write(PIPELINE, CrmDealStage.CRM_DEAL_STAGE_WON))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("paid, signed");
    }

    @Test
    @DisplayName("an unmapped stage, or a pipeline with no source, cannot be written")
    void refusesUnmappedStage() {
        var table = new CrmStageTable(List.of(source(PIPELINE, "dealstage")), List.of());

        assertThatThrownBy(() -> table.write(PIPELINE, CrmDealStage.CRM_DEAL_STAGE_LOST))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not mapped");
        assertThatThrownBy(() -> table.write("unknown", CrmDealStage.CRM_DEAL_STAGE_LOST))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no stage source");
    }

    @Test
    @DisplayName("every source property is read on the deals, once")
    void propertyNames() {
        var table = new CrmStageTable(
                List.of(source(PIPELINE, "dealstage"), source("renewals", "dealstage"), source("other", "bdz_stage")),
                List.of());

        assertThat(table.propertyNames()).containsExactly("bdz_stage", "dealstage");
    }

    private static CrmDealRecord record(String pipelineId, Map<String, String> properties) {
        return new CrmDealRecord(CrmDeal.builder().id("1").pipelineId(pipelineId).build(), properties);
    }

    private static CrmStageSourceEntity source(String pipelineId, String propertyName) {
        var source = new CrmStageSourceEntity();
        source.setPipelineId(pipelineId);
        source.setPropertyName(propertyName);
        return source;
    }

    private static CrmStageMappingEntity mapping(String pipelineId, String value, CrmDealStage stage, boolean writeDefault) {
        var mapping = new CrmStageMappingEntity();
        mapping.setPipelineId(pipelineId);
        mapping.setValue(value);
        mapping.setStage(stage);
        mapping.setWriteDefault(writeDefault);
        return mapping;
    }
}
