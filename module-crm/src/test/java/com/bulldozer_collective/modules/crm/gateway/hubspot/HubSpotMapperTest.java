package com.bulldozer_collective.modules.crm.gateway.hubspot;

import com.bulldozer_collective.modules.crm.activities.services.CreateCrmActivityBuilder;
import com.bulldozer_collective.modules.crm.api.CrmActivityType;
import com.bulldozer_collective.modules.crm.api.CrmLeadStage;
import com.bulldozer_collective.modules.crm.contacts.services.SaveCrmContactBuilder;
import com.bulldozer_collective.modules.crm.deals.services.SaveCrmDealBuilder;
import com.bulldozer_collective.modules.crm.gateway.CrmStageWrite;
import com.bulldozer_collective.modules.crm.gateway.hubspot.dto.HubSpotObject;
import com.bulldozer_collective.modules.crm.leads.services.CreateCrmLeadBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HubSpotMapperTest {

    @Test
    @DisplayName("a deal carries its pipeline and every property read with it, null values included")
    void dealKeepsPipelineAndRawProperties() {
        var properties = new HashMap<String, String>();
        properties.put("dealname", "Acme");
        properties.put("amount", "1200.50");
        properties.put("closedate", "2026-11-30T00:00:00Z");
        properties.put("pipeline", "default");
        properties.put("bdz_stage", "signed");
        properties.put("deal_currency_code", null);

        var record = HubSpotMapper.deal(new HubSpotObject("7", properties, null, null));

        assertThat(record.containerRemoteId()).isEqualTo("default");
        assertThat(record.item().amount()).isEqualByComparingTo(new BigDecimal("1200.50"));
        assertThat(record.item().closeDate()).isEqualTo(Instant.parse("2026-11-30T00:00:00Z"));
        assertThat(record.item().stage()).isNull();
        assertThat(record.item().containerId()).isNull();
        assertThat(record.properties()).containsEntry("bdz_stage", "signed");
    }

    @Test
    @DisplayName("a contact's legal basis is written to hs_legal_basis, and nothing when absent")
    void contactLegalBasis() {
        var withBasis = HubSpotMapper.contact(SaveCrmContactBuilder.builder()
                .email("a@b.c").legalBasis("Legitimate interest – prospect/lead").build());
        var without = HubSpotMapper.contact(SaveCrmContactBuilder.builder().email("a@b.c").build());

        assertThat(withBasis).containsEntry("hs_legal_basis", "Legitimate interest – prospect/lead");
        assertThat(without).doesNotContainKey("hs_legal_basis");
        assertThat(SaveCrmContactBuilder.builder().legalBasis("x").build().isEmpty()).isTrue();
    }

    @Test
    @DisplayName("an unparsable amount or date reads as null rather than failing the whole listing")
    void lenientParsing() {
        var properties = new HashMap<String, String>();
        properties.put("amount", "n/a");
        properties.put("closedate", "1764460800000");

        var deal = HubSpotMapper.deal(new HubSpotObject("7", properties, null, null)).item();

        assertThat(deal.amount()).isNull();
        assertThat(deal.closeDate()).isEqualTo(Instant.ofEpochMilli(1764460800000L));
        assertThat(HubSpotMapper.instant("yesterday")).isNull();
    }

    @Test
    @DisplayName("a deal is created in its container's pipeline with every stage value; an update never touches the pipeline")
    void dealWrite() {
        var deal = SaveCrmDealBuilder.builder().name("Acme").amount(new BigDecimal("10")).build();
        var stages = List.of(new CrmStageWrite("bdz_stage", "signed"), new CrmStageWrite("dealstage", "123"));

        assertThat(HubSpotMapper.deal(deal, "default", stages))
                .containsOnlyKeys("dealname", "amount", "pipeline", "bdz_stage", "dealstage")
                .containsEntry("pipeline", "default")
                .containsEntry("dealstage", "123");
        assertThat(HubSpotMapper.deal(deal, null, List.of())).containsOnlyKeys("dealname", "amount");
    }

    @Test
    @DisplayName("a lead carries its pipeline and its name, type and stage source")
    void leadReadAndWrite() {
        var record = HubSpotMapper.lead(new HubSpotObject("9",
                Map.of("hs_lead_name", "Jane", "hs_lead_type", "NEW_BUSINESS", "hs_pipeline", "lead-pipeline"),
                null, null));
        assertThat(record.containerRemoteId()).isEqualTo("lead-pipeline");
        assertThat(record.item().name()).isEqualTo("Jane");
        assertThat(record.item().type()).isEqualTo("NEW_BUSINESS");

        var lead = CreateCrmLeadBuilder.builder()
                .name("Jane").contactId("42").stage(CrmLeadStage.CRM_LEAD_STAGE_NEW).build();
        assertThat(HubSpotMapper.lead(lead, "lead-pipeline", List.of(new CrmStageWrite("hs_pipeline_stage", "new"))))
                .containsOnlyKeys("hs_lead_name", "hs_pipeline", "hs_pipeline_stage");
    }

    @Test
    @DisplayName("a note has neither title nor status in HubSpot: sending one is refused")
    void noteRejectsTitle() {
        var note = CreateCrmActivityBuilder.builder()
                .type(CrmActivityType.CRM_ACTIVITY_TYPE_NOTE)
                .timestamp(Instant.parse("2026-10-09T10:00:00Z"))
                .title("Call recap")
                .associations(List.of())
                .build();

        assertThatThrownBy(() -> HubSpotMapper.activity(note)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("an activity write always carries hs_timestamp, the only property HubSpot requires")
    void activityWrite() {
        var task = CreateCrmActivityBuilder.builder()
                .type(CrmActivityType.CRM_ACTIVITY_TYPE_TASK)
                .timestamp(Instant.parse("2026-10-09T10:00:00Z"))
                .title("Send the quote")
                .status("NOT_STARTED")
                .associations(List.of())
                .build();

        assertThat(HubSpotMapper.activity(task))
                .containsEntry("hs_timestamp", "2026-10-09T10:00:00Z")
                .containsEntry("hs_task_subject", "Send the quote")
                .containsEntry("hs_task_status", "NOT_STARTED");
    }
}
