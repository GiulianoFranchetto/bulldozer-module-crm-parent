package com.bulldozer_collective.modules.crm.gateway.hubspot;

import com.bulldozer_collective.modules.crm.api.CrmFormFieldOption;
import com.bulldozer_collective.modules.crm.api.CrmFormFieldType;
import com.bulldozer_collective.modules.crm.api.CrmFormLawfulBasis;
import com.bulldozer_collective.modules.crm.api.CrmFormLegalConsent;
import com.bulldozer_collective.modules.crm.api.CrmFormLegalConsentType;
import com.bulldozer_collective.modules.crm.api.CrmObjectType;
import com.bulldozer_collective.modules.crm.forms.services.SaveCrmFormBuilder;
import com.bulldozer_collective.modules.crm.gateway.hubspot.dto.HubSpotForm;
import com.bulldozer_collective.modules.crm.gateway.hubspot.dto.HubSpotFormConfiguration;
import com.bulldozer_collective.modules.crm.gateway.hubspot.dto.HubSpotFormDisplayOptions;
import com.bulldozer_collective.modules.crm.gateway.hubspot.dto.HubSpotFormField;
import com.bulldozer_collective.modules.crm.gateway.hubspot.dto.HubSpotFormFieldGroup;
import com.bulldozer_collective.modules.crm.gateway.hubspot.dto.HubSpotFormLegalConsent;
import com.bulldozer_collective.modules.crm.gateway.hubspot.dto.HubSpotFormPostSubmitAction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HubSpotFormMapperTest {

    @Test
    @DisplayName("a form's rows are flattened into its fields; a type the bridge does not cover reads as OTHER")
    void readsFields() {
        var form = new HubSpotForm("f1", "Contact us", "2026-10-01T00:00:00Z", null, List.of(
                new HubSpotFormFieldGroup(List.of(
                        new HubSpotFormField("0-1", "email", "Email", "email", true, null),
                        new HubSpotFormField("0-2", "name", "Company", "single_line_text", false, null))),
                new HubSpotFormFieldGroup(List.of(
                        new HubSpotFormField("0-1", "cv", "CV", "file", false, null)))),
                new HubSpotFormConfiguration("fr", new HubSpotFormPostSubmitAction("redirect_url", "https://x.io/merci")),
                new HubSpotFormDisplayOptions("Envoyer"),
                new HubSpotFormLegalConsent("legitimate_interest", "lead", "We use your data to…"));

        var read = HubSpotFormMapper.form(form);

        assertThat(read.fields()).extracting(field -> field.fieldType()).containsExactly(
                CrmFormFieldType.CRM_FORM_FIELD_TYPE_EMAIL,
                CrmFormFieldType.CRM_FORM_FIELD_TYPE_TEXT,
                CrmFormFieldType.CRM_FORM_FIELD_TYPE_OTHER);
        assertThat(read.fields().get(1).objectType()).isEqualTo(CrmObjectType.CRM_OBJECT_TYPE_COMPANY);
        assertThat(read.redirectUrl()).isEqualTo("https://x.io/merci");
        assertThat(read.thankYouMessage()).isNull();
        assertThat(read.legalConsent().lawfulBasis()).isEqualTo(CrmFormLawfulBasis.CRM_FORM_LAWFUL_BASIS_LEAD);
    }

    @Test
    @DisplayName("an explicit consent reads as OTHER, keeping its privacy text")
    void otherConsent() {
        var consent = HubSpotFormMapper.legalConsent(
                new HubSpotFormLegalConsent("explicit_consent_to_process", null, "Privacy"));

        assertThat(consent.type()).isEqualTo(CrmFormLegalConsentType.CRM_FORM_LEGAL_CONSENT_TYPE_OTHER);
        assertThat(consent.privacyText()).isEqualTo("Privacy");
    }

    @Test
    @DisplayName("a created form is a HubSpot form, each field a row of its own, options numbered in order")
    @SuppressWarnings("unchecked")
    void createBody() {
        var body = HubSpotFormMapper.form(SaveCrmFormBuilder.builder()
                .name("Contact us")
                .fields(List.of(
                        field("email", CrmFormFieldType.CRM_FORM_FIELD_TYPE_EMAIL, List.of()),
                        field("source", CrmFormFieldType.CRM_FORM_FIELD_TYPE_SELECT, List.of(
                                new CrmFormFieldOption("Web", "web"), new CrmFormFieldOption("Salon", "fair")))))
                .thankYouMessage("Merci")
                .legalConsent(new CrmFormLegalConsent(CrmFormLegalConsentType.CRM_FORM_LEGAL_CONSENT_TYPE_LEGITIMATE_INTEREST,
                        CrmFormLawfulBasis.CRM_FORM_LAWFUL_BASIS_LEAD, "We use your data to…"))
                .build(), true);

        assertThat(body).containsEntry("formType", "hubspot").containsEntry("archived", false);
        var groups = (List<Map<String, Object>>) body.get("fieldGroups");
        assertThat(groups).hasSize(2).allSatisfy(group -> assertThat(group).containsEntry("groupType", "default_group"));
        var email = ((List<Map<String, Object>>) groups.get(0).get("fields")).get(0);
        assertThat(email).containsEntry("fieldType", "email").containsEntry("objectTypeId", "0-1").containsKey("validation");
        var source = ((List<Map<String, Object>>) groups.get(1).get("fields")).get(0);
        assertThat(source).containsEntry("fieldType", "dropdown").containsEntry("options", List.of(
                Map.of("label", "Web", "value", "web", "displayOrder", 0),
                Map.of("label", "Salon", "value", "fair", "displayOrder", 1)));
        assertThat((Map<String, Object>) body.get("configuration"))
                .containsEntry("postSubmitAction", Map.of("type", "thank_you", "value", "Merci"));
        assertThat((Map<String, Object>) body.get("legalConsentOptions"))
                .containsEntry("type", "legitimate_interest").containsEntry("lawfulBasis", "lead");
    }

    @Test
    @DisplayName("an update sends only the parts given")
    void updateBody() {
        var body = HubSpotFormMapper.form(SaveCrmFormBuilder.builder().name("Renamed").build(), false);

        assertThat(body).containsOnlyKeys("name");
    }

    private static SaveCrmFormBuilder.Field field(String name, CrmFormFieldType type, List<CrmFormFieldOption> options) {
        return new SaveCrmFormBuilder.Field(CrmObjectType.CRM_OBJECT_TYPE_CONTACT, name, name, type, true, options);
    }
}
