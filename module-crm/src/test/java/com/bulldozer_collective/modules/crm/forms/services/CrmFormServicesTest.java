package com.bulldozer_collective.modules.crm.forms.services;

import com.bulldozer_collective.logging.BulldozerLogger;
import com.bulldozer_collective.modules.crm.api.CrmForm;
import com.bulldozer_collective.modules.crm.api.CrmFormField;
import com.bulldozer_collective.modules.crm.api.CrmFormFieldOption;
import com.bulldozer_collective.modules.crm.api.CrmFormFieldType;
import com.bulldozer_collective.modules.crm.api.CrmFormLawfulBasis;
import com.bulldozer_collective.modules.crm.api.CrmFormLegalConsent;
import com.bulldozer_collective.modules.crm.api.CrmFormLegalConsentType;
import com.bulldozer_collective.modules.crm.api.CrmObjectType;
import com.bulldozer_collective.modules.crm.gateway.CrmGateway;
import com.bulldozer_collective.modules.crm.gateway.CrmGatewayResolver;
import com.bulldozer_collective.modules.crm.gateway.CrmSession;
import com.bulldozer_collective.modules.crm.gateway.CrmSessionOpener;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** A form is checked before the CRM is reached: a refused one never opens a session, nor half-writes. */
class CrmFormServicesTest {

    private static final UUID CUSTOMER = UUID.randomUUID();
    private static final UUID PROJECT = UUID.randomUUID();
    private static final UUID CONNECTION = UUID.randomUUID();

    private final CrmSessionOpener sessionOpener = mock(CrmSessionOpener.class);
    private final CrmGatewayResolver gatewayResolver = mock(CrmGatewayResolver.class);
    private final CrmGateway gateway = mock(CrmGateway.class);
    private final CrmFormFactory factory =
            new CrmFormFactory(this.sessionOpener, this.gatewayResolver, mock(BulldozerLogger.class));
    private final CrmFormUpdater updater =
            new CrmFormUpdater(this.sessionOpener, this.gatewayResolver, mock(BulldozerLogger.class));

    @Test
    @DisplayName("a dropdown without options is refused")
    void selectNeedsOptions() throws Exception {
        this.assertCreateRefused(form(field(CrmFormFieldType.CRM_FORM_FIELD_TYPE_SELECT, List.of())), "needs options");
    }

    @Test
    @DisplayName("options on a text field are refused")
    void onlyChoicesTakeOptions() throws Exception {
        this.assertCreateRefused(form(field(CrmFormFieldType.CRM_FORM_FIELD_TYPE_TEXT,
                List.of(new CrmFormFieldOption("A", "a")))), "only a SELECT or RADIO");
    }

    @Test
    @DisplayName("a field writing to a deal is refused")
    void contactOrCompanyOnly() throws Exception {
        var deal = new SaveCrmFormBuilder.Field(CrmObjectType.CRM_OBJECT_TYPE_DEAL, "amount", "Amount",
                CrmFormFieldType.CRM_FORM_FIELD_TYPE_NUMBER, false, List.of());
        this.assertCreateRefused(form(deal), "contact or a company only");
    }

    @Test
    @DisplayName("a thank-you message and a redirection together are refused")
    void oneAfterSubmitAction() throws Exception {
        var form = form(field(CrmFormFieldType.CRM_FORM_FIELD_TYPE_EMAIL, List.of())).toBuilder()
                .thankYouMessage("Merci").redirectUrl("https://x.io").build();
        this.assertCreateRefused(form, "not both");
    }

    @Test
    @DisplayName("a legitimate interest without a privacy text is refused: the bridge never fills a legal notice")
    void legitimateInterestNeedsText() throws Exception {
        var form = form(field(CrmFormFieldType.CRM_FORM_FIELD_TYPE_EMAIL, List.of())).toBuilder()
                .legalConsent(new CrmFormLegalConsent(CrmFormLegalConsentType.CRM_FORM_LEGAL_CONSENT_TYPE_LEGITIMATE_INTEREST,
                        CrmFormLawfulBasis.CRM_FORM_LAWFUL_BASIS_LEAD, null))
                .build();
        this.assertCreateRefused(form, "privacyText");
    }

    @Test
    @DisplayName("replacing the fields of a form holding an uncovered field is refused, nothing written")
    void keepsUncoveredFields() throws Exception {
        var session = mock(CrmSession.class);
        when(this.sessionOpener.open(CUSTOMER, PROJECT, CONNECTION)).thenReturn(session);
        when(this.gatewayResolver.resolve(session)).thenReturn(this.gateway);
        when(this.gateway.getForm(session, "f1")).thenReturn(CrmForm.builder()
                .id("f1").name("Form")
                .fields(List.of(CrmFormField.builder().name("cv").label("CV")
                        .fieldType(CrmFormFieldType.CRM_FORM_FIELD_TYPE_OTHER).options(List.of()).build()))
                .legalConsent(new CrmFormLegalConsent(CrmFormLegalConsentType.CRM_FORM_LEGAL_CONSENT_TYPE_NONE, null, null))
                .build());

        var update = SaveCrmFormBuilder.builder()
                .fields(List.of(field(CrmFormFieldType.CRM_FORM_FIELD_TYPE_EMAIL, List.of())))
                .build();

        assertThatThrownBy(() -> this.updater.update(CUSTOMER, PROJECT, CONNECTION, "f1", update))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CRM_FORM_FIELD_TYPE_OTHER");
        verify(this.gateway, never()).updateForm(any(), eq("f1"), any());
    }

    private void assertCreateRefused(SaveCrmFormBuilder form, String message) throws Exception {
        assertThatThrownBy(() -> this.factory.create(CUSTOMER, PROJECT, CONNECTION, form))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(message);
        verify(this.sessionOpener, never()).open(any(), any(), any());
    }

    private static SaveCrmFormBuilder form(SaveCrmFormBuilder.Field field) {
        return SaveCrmFormBuilder.builder().name("Contact us").fields(List.of(field)).build();
    }

    private static SaveCrmFormBuilder.Field field(CrmFormFieldType type, List<CrmFormFieldOption> options) {
        return new SaveCrmFormBuilder.Field(CrmObjectType.CRM_OBJECT_TYPE_CONTACT, "field", "Field", type, false, options);
    }
}
