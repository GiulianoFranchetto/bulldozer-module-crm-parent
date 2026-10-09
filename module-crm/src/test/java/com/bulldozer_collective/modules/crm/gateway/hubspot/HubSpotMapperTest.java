package com.bulldozer_collective.modules.crm.gateway.hubspot;

import com.bulldozer_collective.modules.crm.activities.services.CreateCrmActivityBuilder;
import com.bulldozer_collective.modules.crm.api.CrmActivityType;
import com.bulldozer_collective.modules.crm.deals.services.SaveCrmDealBuilder;
import com.bulldozer_collective.modules.crm.gateway.CrmStageWrite;
import com.bulldozer_collective.modules.crm.gateway.hubspot.dto.HubSpotObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HubSpotMapperTest {

    @Test
    @DisplayName("a deal keeps every property it was read with, null values included, for the stage resolution")
    void dealKeepsRawProperties() {
        var properties = new HashMap<String, String>();
        properties.put("dealname", "Acme");
        properties.put("amount", "1200.50");
        properties.put("closedate", "2026-11-30T00:00:00Z");
        properties.put("pipeline", "default");
        properties.put("bdz_stage", "signed");
        properties.put("deal_currency_code", null);

        var record = HubSpotMapper.deal(new HubSpotObject("7", properties, null, null));

        assertThat(record.deal().amount()).isEqualByComparingTo(new BigDecimal("1200.50"));
        assertThat(record.deal().closeDate()).isEqualTo(Instant.parse("2026-11-30T00:00:00Z"));
        assertThat(record.deal().pipelineId()).isEqualTo("default");
        assertThat(record.deal().stage()).isNull();
        assertThat(record.properties()).containsEntry("bdz_stage", "signed");
    }

    @Test
    @DisplayName("an unparsable amount or date reads as null rather than failing the whole listing")
    void lenientParsing() {
        var properties = new HashMap<String, String>();
        properties.put("amount", "n/a");
        properties.put("closedate", "1764460800000");

        var deal = HubSpotMapper.deal(new HubSpotObject("7", properties, null, null)).deal();

        assertThat(deal.amount()).isNull();
        assertThat(deal.closeDate()).isEqualTo(Instant.ofEpochMilli(1764460800000L));
        assertThat(HubSpotMapper.instant("yesterday")).isNull();
    }

    @Test
    @DisplayName("a deal write carries only the given fields, plus the stage property and value")
    void dealWrite() {
        var deal = SaveCrmDealBuilder.builder().name("Acme").amount(new BigDecimal("10")).build();

        var properties = HubSpotMapper.deal(deal, new CrmStageWrite("bdz_stage", "signed"));

        assertThat(properties).containsOnlyKeys("dealname", "amount", "bdz_stage");
        assertThat(properties).containsEntry("bdz_stage", "signed");
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
