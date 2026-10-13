package com.bulldozer_collective.modules.crm.gateway.hubspot;

import com.bulldozer_collective.modules.crm.activities.services.CreateCrmActivityBuilder;
import com.bulldozer_collective.modules.crm.api.CrmActivity;
import com.bulldozer_collective.modules.crm.api.CrmActivityType;
import com.bulldozer_collective.modules.crm.api.CrmCompany;
import com.bulldozer_collective.modules.crm.api.CrmContact;
import com.bulldozer_collective.modules.crm.api.CrmDeal;
import com.bulldozer_collective.modules.crm.api.CrmLead;
import com.bulldozer_collective.modules.crm.api.CrmProperty;
import com.bulldozer_collective.modules.crm.api.CrmPropertyOption;
import com.bulldozer_collective.modules.crm.gateway.CrmRemoteContainer;
import com.bulldozer_collective.modules.crm.gateway.CrmRemoteContainerStage;
import com.bulldozer_collective.modules.crm.companies.services.SaveCrmCompanyBuilder;
import com.bulldozer_collective.modules.crm.contacts.services.SaveCrmContactBuilder;
import com.bulldozer_collective.modules.crm.deals.services.SaveCrmDealBuilder;
import com.bulldozer_collective.modules.crm.gateway.CrmContainedRecord;
import com.bulldozer_collective.modules.crm.gateway.CrmStageWrite;
import com.bulldozer_collective.modules.crm.gateway.hubspot.dto.HubSpotObject;
import com.bulldozer_collective.modules.crm.gateway.hubspot.dto.HubSpotPipeline;
import com.bulldozer_collective.modules.crm.gateway.hubspot.dto.HubSpotProperty;
import com.bulldozer_collective.modules.crm.leads.services.CreateCrmLeadBuilder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Translates between HubSpot's records and the module's model. Property names are HubSpot's default ones, as
 * listed in its object and activity guides.
 */
final class HubSpotMapper {

    static final List<String> CONTACT_PROPERTIES = List.of("firstname", "lastname", "email", "phone", "jobtitle");
    static final List<String> COMPANY_PROPERTIES = List.of("name", "domain", "industry", "phone", "city", "country");
    static final List<String> DEAL_PROPERTIES = List.of(
            "dealname", "amount", "deal_currency_code", "closedate", HubSpotContainerKind.DEALS.pipelineProperty());
    /** The leads guide names {@code hs_lead_name} (required on create) and {@code hs_lead_type}. */
    static final List<String> LEAD_PROPERTIES = List.of(
            "hs_lead_name", "hs_lead_type", HubSpotContainerKind.LEADS.pipelineProperty());

    private HubSpotMapper() {
    }

    // ── read ─────────────────────────────────────────────────────────────────

    static @NonNull CrmContact contact(@NonNull HubSpotObject object) {
        var properties = properties(object);
        return CrmContact.builder()
                .id(Objects.requireNonNull(object.id()))
                .firstName(properties.get("firstname"))
                .lastName(properties.get("lastname"))
                .email(properties.get("email"))
                .phone(properties.get("phone"))
                .jobTitle(properties.get("jobtitle"))
                .createdAt(object.createdAt())
                .updatedAt(object.updatedAt())
                .build();
    }

    static @NonNull CrmCompany company(@NonNull HubSpotObject object) {
        var properties = properties(object);
        return CrmCompany.builder()
                .id(Objects.requireNonNull(object.id()))
                .name(properties.get("name"))
                .domain(properties.get("domain"))
                .industry(properties.get("industry"))
                .phone(properties.get("phone"))
                .city(properties.get("city"))
                .country(properties.get("country"))
                .createdAt(object.createdAt())
                .updatedAt(object.updatedAt())
                .build();
    }

    static @NonNull CrmContainedRecord<CrmDeal> deal(@NonNull HubSpotObject object) {
        var properties = properties(object);
        var deal = CrmDeal.builder()
                .id(Objects.requireNonNull(object.id()))
                .name(properties.get("dealname"))
                .amount(decimal(properties.get("amount")))
                .currency(properties.get("deal_currency_code"))
                .closeDate(instant(properties.get("closedate")))
                .createdAt(object.createdAt())
                .updatedAt(object.updatedAt())
                .build();
        return new CrmContainedRecord<>(deal, properties.get(HubSpotContainerKind.DEALS.pipelineProperty()),
                properties);
    }

    static @NonNull CrmContainedRecord<CrmLead> lead(@NonNull HubSpotObject object) {
        var properties = properties(object);
        var lead = CrmLead.builder()
                .id(Objects.requireNonNull(object.id()))
                .name(properties.get("hs_lead_name"))
                .type(properties.get("hs_lead_type"))
                .createdAt(object.createdAt())
                .updatedAt(object.updatedAt())
                .build();
        return new CrmContainedRecord<>(lead, properties.get(HubSpotContainerKind.LEADS.pipelineProperty()),
                properties);
    }

    static @NonNull CrmActivity activity(@NonNull CrmActivityType type, @NonNull HubSpotObject object) {
        var properties = properties(object);
        var names = ActivityProperties.of(type);
        return CrmActivity.builder()
                .id(Objects.requireNonNull(object.id()))
                .type(type)
                .title(names.title() == null ? null : properties.get(names.title()))
                .body(properties.get(names.body()))
                .status(names.status() == null ? null : properties.get(names.status()))
                .timestamp(instant(properties.get(ActivityProperties.TIMESTAMP)))
                .createdAt(object.createdAt())
                .updatedAt(object.updatedAt())
                .build();
    }

    static @NonNull CrmRemoteContainer pipeline(@NonNull HubSpotPipeline pipeline) {
        var stages = pipeline.stages() == null ? List.<CrmRemoteContainerStage>of() : pipeline.stages().stream()
                .filter(stage -> stage.id() != null && !Boolean.TRUE.equals(stage.archived()))
                .map(stage -> CrmRemoteContainerStage.builder()
                        .remoteId(stage.id())
                        .name(stage.label() == null ? stage.id() : stage.label())
                        .displayOrder(stage.displayOrder() == null ? 0 : stage.displayOrder())
                        .build())
                .sorted(Comparator.comparingInt(CrmRemoteContainerStage::displayOrder))
                .toList();
        return CrmRemoteContainer.builder()
                .remoteId(Objects.requireNonNull(pipeline.id()))
                .name(pipeline.label() == null ? pipeline.id() : pipeline.label())
                .displayOrder(pipeline.displayOrder() == null ? 0 : pipeline.displayOrder())
                .stages(stages)
                .build();
    }

    static @NonNull CrmProperty property(@NonNull HubSpotProperty property) {
        var options = property.options() == null ? List.<CrmPropertyOption>of() : property.options().stream()
                .filter(option -> option.value() != null)
                .map(option -> CrmPropertyOption.builder()
                        .value(option.value())
                        .label(option.label() == null ? option.value() : option.label())
                        .displayOrder(option.displayOrder() == null ? 0 : option.displayOrder())
                        .hidden(Boolean.TRUE.equals(option.hidden()))
                        .build())
                .sorted(Comparator.comparingInt(CrmPropertyOption::displayOrder))
                .toList();
        var name = Objects.requireNonNull(property.name());
        return CrmProperty.builder()
                .name(name)
                .label(property.label() == null ? name : property.label())
                .type(property.type() == null ? "string" : property.type())
                .fieldType(property.fieldType())
                .readOnly(property.modificationMetadata() != null
                        && Boolean.TRUE.equals(property.modificationMetadata().readOnlyValue()))
                .options(options)
                .build();
    }

    // ── write ────────────────────────────────────────────────────────────────

    static @NonNull Map<String, String> contact(@NonNull SaveCrmContactBuilder contact) {
        var properties = new LinkedHashMap<String, String>();
        put(properties, "firstname", contact.firstName());
        put(properties, "lastname", contact.lastName());
        put(properties, "email", contact.email());
        put(properties, "phone", contact.phone());
        put(properties, "jobtitle", contact.jobTitle());
        put(properties, "hs_legal_basis", contact.legalBasis());
        return properties;
    }

    static @NonNull Map<String, String> company(@NonNull SaveCrmCompanyBuilder company) {
        var properties = new LinkedHashMap<String, String>();
        put(properties, "name", company.name());
        put(properties, "domain", company.domain());
        put(properties, "industry", company.industry());
        put(properties, "phone", company.phone());
        put(properties, "city", company.city());
        put(properties, "country", company.country());
        return properties;
    }

    /** @param containerRemoteId the pipeline to create the deal in; {@code null} on an update, which never moves it. */
    static @NonNull Map<String, String> deal(@NonNull SaveCrmDealBuilder deal,
                                             @Nullable String containerRemoteId,
                                             @NonNull List<CrmStageWrite> stages) {
        var properties = new LinkedHashMap<String, String>();
        put(properties, "dealname", deal.name());
        put(properties, "amount", deal.amount() == null ? null : deal.amount().toPlainString());
        put(properties, "deal_currency_code", deal.currency());
        put(properties, "closedate", deal.closeDate() == null ? null : deal.closeDate().toString());
        put(properties, HubSpotContainerKind.DEALS.pipelineProperty(), containerRemoteId);
        stages.forEach(stage -> properties.put(stage.propertyName(), stage.value()));
        return properties;
    }

    static @NonNull Map<String, String> lead(@NonNull CreateCrmLeadBuilder lead,
                                             @NonNull String containerRemoteId,
                                             @NonNull List<CrmStageWrite> stages) {
        var properties = new LinkedHashMap<String, String>();
        properties.put("hs_lead_name", lead.name());
        put(properties, "hs_lead_type", lead.type());
        properties.put(HubSpotContainerKind.LEADS.pipelineProperty(), containerRemoteId);
        stages.forEach(stage -> properties.put(stage.propertyName(), stage.value()));
        return properties;
    }

    /** @throws IllegalArgumentException when a field the activity type has no property for is given. */
    static @NonNull Map<String, String> activity(@NonNull CreateCrmActivityBuilder activity) {
        var names = ActivityProperties.of(activity.type());
        if (activity.title() != null && names.title() == null) {
            throw new IllegalArgumentException("title is not supported for a %s".formatted(activity.type()));
        }
        if (activity.status() != null && names.status() == null) {
            throw new IllegalArgumentException("status is not supported for a %s".formatted(activity.type()));
        }

        var properties = new LinkedHashMap<String, String>();
        properties.put(ActivityProperties.TIMESTAMP, activity.timestamp().toString());
        if (names.title() != null) {
            put(properties, names.title(), activity.title());
        }
        put(properties, names.body(), activity.body());
        if (names.status() != null) {
            put(properties, names.status(), activity.status());
        }
        return properties;
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private static Map<String, String> properties(@NonNull HubSpotObject object) {
        // HashMap rather than Map.copyOf: HubSpot sends null property values, which Map.copyOf rejects.
        return object.properties() == null ? Map.of() : new HashMap<>(object.properties());
    }

    private static void put(@NonNull Map<String, String> properties, @NonNull String name, @Nullable String value) {
        if (value != null) {
            properties.put(name, value);
        }
    }

    private static @Nullable BigDecimal decimal(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** HubSpot serialises datetimes as ISO-8601 strings; older records may still carry epoch milliseconds. */
    static @Nullable Instant instant(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value.trim());
        } catch (DateTimeParseException e) {
            try {
                return Instant.ofEpochMilli(Long.parseLong(value.trim()));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
    }

    /**
     * The HubSpot properties holding an activity's title, body and status. Only {@code hs_timestamp} is required
     * by HubSpot; a note has neither title nor status.
     */
    record ActivityProperties(@Nullable String title, @NonNull String body, @Nullable String status) {

        static final String TIMESTAMP = "hs_timestamp";

        static ActivityProperties of(@NonNull CrmActivityType type) {
            return switch (type) {
                case CRM_ACTIVITY_TYPE_CALL -> new ActivityProperties("hs_call_title", "hs_call_body", "hs_call_status");
                case CRM_ACTIVITY_TYPE_EMAIL ->
                        new ActivityProperties("hs_email_subject", "hs_email_text", "hs_email_status");
                case CRM_ACTIVITY_TYPE_MEETING ->
                        new ActivityProperties("hs_meeting_title", "hs_meeting_body", "hs_meeting_outcome");
                case CRM_ACTIVITY_TYPE_NOTE -> new ActivityProperties(null, "hs_note_body", null);
                case CRM_ACTIVITY_TYPE_TASK -> new ActivityProperties("hs_task_subject", "hs_task_body", "hs_task_status");
            };
        }

        List<String> all() {
            var names = new ArrayList<String>();
            names.add(TIMESTAMP);
            if (this.title != null) {
                names.add(this.title);
            }
            names.add(this.body);
            if (this.status != null) {
                names.add(this.status);
            }
            return names;
        }
    }
}
