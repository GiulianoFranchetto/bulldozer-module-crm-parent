package com.bulldozer_collective.modules.crm.imports.services;

import com.bulldozer_collective.companies.api.Company;
import com.bulldozer_collective.contacts.api.Contact;
import com.bulldozer_collective.contacts.api.ContactJob;
import com.bulldozer_collective.logging.BulldozerLogger;
import com.bulldozer_collective.modules.crm.api.CrmCompany;
import com.bulldozer_collective.modules.crm.api.CrmContact;
import com.bulldozer_collective.modules.crm.api.CrmImportCounts;
import com.bulldozer_collective.modules.crm.api.CrmObjectType;
import com.bulldozer_collective.modules.crm.companies.services.SaveCrmCompanyBuilder;
import com.bulldozer_collective.modules.crm.contacts.services.SaveCrmContactBuilder;
import com.bulldozer_collective.modules.crm.gateway.CrmBulkResult;
import com.bulldozer_collective.modules.crm.gateway.CrmGateway;
import com.bulldozer_collective.modules.crm.gateway.CrmGatewayResolver;
import com.bulldozer_collective.modules.crm.gateway.CrmLink;
import com.bulldozer_collective.modules.crm.gateway.CrmNote;
import com.bulldozer_collective.modules.crm.gateway.CrmSession;
import com.bulldozer_collective.modules.crm.gateway.CrmSessionOpener;
import com.bulldozer_collective.modules.outbound.lead_lists.Lead;
import com.bulldozer_collective.modules.outbound.lead_lists.LeadType;
import com.bulldozer_collective.shared.Address;
import com.bulldozer_collective.shared.IntegrationPlatform;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** An import fills only what the CRM lacks, and notes only what it changed. */
class CrmLeadListImporterTest {

    private static final UUID CUSTOMER = UUID.randomUUID();
    private static final UUID PROJECT = UUID.randomUUID();
    private static final UUID CONNECTION = UUID.randomUUID();
    private static final UUID LIST = UUID.randomUUID();
    private static final Instant NOW = Instant.now();

    private final CrmLeadListSource source = mock(CrmLeadListSource.class);
    private final CrmSessionOpener sessionOpener = mock(CrmSessionOpener.class);
    private final CrmGatewayResolver gatewayResolver = mock(CrmGatewayResolver.class);
    private final CrmGateway gateway = mock(CrmGateway.class);
    private final CrmLeadListImporter importer =
            new CrmLeadListImporter(this.source, this.sessionOpener, this.gatewayResolver, mock(BulldozerLogger.class));

    @BeforeEach
    void setUp() throws Exception {
        var session = CrmSession.builder().customerId(CUSTOMER).projectId(PROJECT).connectionId(CONNECTION)
                .platform(IntegrationPlatform.INTEGRATION_PLATFORM_HUBSPOT).accessToken("token").build();
        when(this.sessionOpener.open(CUSTOMER, PROJECT, CONNECTION)).thenReturn(session);
        when(this.gatewayResolver.resolve(session)).thenReturn(this.gateway);
        when(this.source.name(CUSTOMER, PROJECT, LIST)).thenReturn("Salon <2026>");
        when(this.gateway.findCompaniesByDomain(any(), anyCollection())).thenReturn(List.of());
        when(this.gateway.findContactsByEmail(any(), anyCollection())).thenReturn(List.of());
        when(this.gateway.createCompanies(any(), anyList())).thenReturn(new CrmBulkResult<>(List.of(), Map.of()));
        when(this.gateway.createContacts(any(), anyList())).thenReturn(new CrmBulkResult<>(List.of(), Map.of()));
        when(this.gateway.updateCompanies(any(), anyList(), anyList())).thenReturn(new CrmBulkResult<>(List.of(), Map.of()));
        when(this.gateway.updateContacts(any(), anyList(), anyList())).thenReturn(new CrmBulkResult<>(List.of(), Map.of()));
        when(this.gateway.associateAll(any(), any(), any(), anyList())).thenReturn(List.of());
        when(this.gateway.createNotes(any(), anyList())).thenReturn(new CrmBulkResult<>(List.of(), Map.of()));
    }

    @Test
    @DisplayName("a new contact and its new employer are created, associated, and each gets the list's note")
    void createsWhatTheCrmLacks() throws Exception {
        var acme = company("Acme", "https://www.acme.io/about");
        this.given(List.of(contactLead(contact("Ada", "Lovelace", "Ada@Acme.io", "+33 1", acme.id()))), acme);
        when(this.gateway.createCompanies(any(), anyList())).thenReturn(new CrmBulkResult<>(
                List.of(CrmCompany.builder().id("c1").name("Acme").domain("acme.io").build()), Map.of()));
        when(this.gateway.createContacts(any(), anyList())).thenReturn(new CrmBulkResult<>(
                List.of(CrmContact.builder().id("p1").email("ada@acme.io").build()), Map.of()));

        var response = this.importer.importLeadList(CUSTOMER, PROJECT, CONNECTION, LIST, "Not applicable");

        assertThat(response.companies()).isEqualTo(new CrmImportCounts(1, 0, 0));
        assertThat(response.contacts()).isEqualTo(new CrmImportCounts(1, 0, 0));
        assertThat(response.skipped()).isEmpty();
        var contacts = this.<SaveCrmContactBuilder>captureList();
        verify(this.gateway).createContacts(any(), contacts.capture());
        assertThat(contacts.getValue().get(0).email()).isEqualTo("ada@acme.io");
        assertThat(contacts.getValue().get(0).jobTitle()).isEqualTo("CTO");
        assertThat(contacts.getValue().get(0).legalBasis()).isEqualTo("Not applicable");
        verify(this.gateway).associateAll(any(), eq(CrmObjectType.CRM_OBJECT_TYPE_CONTACT),
                eq(CrmObjectType.CRM_OBJECT_TYPE_COMPANY), eq(List.of(new CrmLink("p1", "c1"))));
        var notes = this.<CrmNote>captureList();
        verify(this.gateway).createNotes(any(), notes.capture());
        assertThat(notes.getValue()).extracting(note -> note.about().id()).containsExactly("c1", "p1");
        assertThat(notes.getValue().get(0).body()).contains("From Salon &lt;2026&gt;", "https://bulldozer-os.fr");
    }

    @Test
    @DisplayName("an existing contact only gets its empty fields; a complete, associated company is left alone")
    void fillsOnlyEmptyFields() throws Exception {
        var acme = company("Acme", "acme.io");
        this.given(List.of(contactLead(contact("Ada", "Lovelace", "ada@acme.io", "+33 1", acme.id()))), acme);
        when(this.gateway.findCompaniesByDomain(any(), anyCollection())).thenReturn(List.of(CrmCompany.builder()
                .id("c1").name("ACME Corp").domain("acme.io").city("Paris").country("France").build()));
        when(this.gateway.findContactsByEmail(any(), anyCollection())).thenReturn(List.of(CrmContact.builder()
                .id("p1").firstName("Ada").lastName("L.").email("ada@acme.io").jobTitle("Founder").build()));
        when(this.gateway.listAssociatedIds(any(), any(), anyCollection(), any())).thenReturn(Map.of("p1", Set.of("c1")));

        var response = this.importer.importLeadList(CUSTOMER, PROJECT, CONNECTION, LIST, null);

        assertThat(response.companies()).isEqualTo(new CrmImportCounts(0, 0, 1));
        assertThat(response.contacts()).isEqualTo(new CrmImportCounts(0, 1, 0));
        var fills = this.<SaveCrmContactBuilder>captureList();
        verify(this.gateway).updateContacts(any(), eq(List.of("p1")), fills.capture());
        assertThat(fills.getValue().get(0)).isEqualTo(SaveCrmContactBuilder.builder().phone("+33 1").build());
        verify(this.gateway, never()).associateAll(any(), any(), any(), anyList());
        var notes = this.<CrmNote>captureList();
        verify(this.gateway).createNotes(any(), notes.capture());
        assertThat(notes.getValue()).extracting(note -> note.about().id()).containsExactly("p1");
    }

    @Test
    @DisplayName("records the CRM already holds in full, and already associated, get nothing: no write, no note")
    void rerunChangesNothing() throws Exception {
        var acme = company("Acme", "acme.io");
        this.given(List.of(contactLead(contact("Ada", "Lovelace", "ada@acme.io", null, acme.id()))), acme);
        when(this.gateway.findCompaniesByDomain(any(), anyCollection())).thenReturn(List.of(CrmCompany.builder()
                .id("c1").name("Acme").domain("acme.io").city("Paris").country("France").build()));
        when(this.gateway.findContactsByEmail(any(), anyCollection())).thenReturn(List.of(CrmContact.builder()
                .id("p1").firstName("Ada").lastName("Lovelace").email("ada@acme.io").jobTitle("CTO").build()));
        when(this.gateway.listAssociatedIds(any(), any(), anyCollection(), any())).thenReturn(Map.of("p1", Set.of("c1")));

        var response = this.importer.importLeadList(CUSTOMER, PROJECT, CONNECTION, LIST, null);

        assertThat(response.companies()).isEqualTo(new CrmImportCounts(0, 0, 1));
        assertThat(response.contacts()).isEqualTo(new CrmImportCounts(0, 0, 1));
        verify(this.gateway, never()).createNotes(any(), anyList());
    }

    @Test
    @DisplayName("an existing contact newly associated with its company counts as changed on both sides")
    void associationIsAChange() throws Exception {
        var acme = company("Acme", "acme.io");
        this.given(List.of(contactLead(contact("Ada", "Lovelace", "ada@acme.io", null, acme.id()))), acme);
        when(this.gateway.findCompaniesByDomain(any(), anyCollection())).thenReturn(List.of(CrmCompany.builder()
                .id("c1").name("Acme").domain("acme.io").city("Paris").country("France").build()));
        when(this.gateway.findContactsByEmail(any(), anyCollection())).thenReturn(List.of(CrmContact.builder()
                .id("p1").firstName("Ada").lastName("Lovelace").email("ada@acme.io").jobTitle("CTO").build()));
        when(this.gateway.listAssociatedIds(any(), any(), anyCollection(), any())).thenReturn(Map.of());

        var response = this.importer.importLeadList(CUSTOMER, PROJECT, CONNECTION, LIST, null);

        assertThat(response.companies()).isEqualTo(new CrmImportCounts(0, 1, 0));
        assertThat(response.contacts()).isEqualTo(new CrmImportCounts(0, 1, 0));
        var notes = this.<CrmNote>captureList();
        verify(this.gateway).createNotes(any(), notes.capture());
        assertThat(notes.getValue()).hasSize(2);
    }

    @Test
    @DisplayName("a company without website matching several CRM companies by name is skipped, never created")
    void ambiguousNameIsSkipped() throws Exception {
        var lead = Lead.builder().id(UUID.randomUUID()).customerId(CUSTOMER).projectId(PROJECT)
                .type(LeadType.LEAD_TYPE_COMPANY).company(company("Acme", null)).leadListIds(List.of(LIST))
                .createdAt(NOW).updatedAt(NOW).build();
        this.given(List.of(lead));
        when(this.gateway.findCompaniesByName(any(), eq("Acme"))).thenReturn(List.of(
                CrmCompany.builder().id("c1").name("Acme").build(), CrmCompany.builder().id("c2").name("Acme").build()));

        var response = this.importer.importLeadList(CUSTOMER, PROJECT, CONNECTION, LIST, null);

        assertThat(response.skipped()).singleElement().satisfies(skip -> {
            assertThat(skip.leadId()).isEqualTo(lead.id());
            assertThat(skip.reason()).contains("2 companies");
        });
        var created = this.<SaveCrmCompanyBuilder>captureList();
        verify(this.gateway).createCompanies(any(), created.capture());
        assertThat(created.getValue()).isEmpty();
    }

    @Test
    @DisplayName("a website becomes a bare domain")
    void domains() {
        assertThat(CrmLeadListImporter.domain("https://www.Acme.io:443/about?x=1")).isEqualTo("acme.io");
        assertThat(CrmLeadListImporter.domain("acme.io")).isEqualTo("acme.io");
        assertThat(CrmLeadListImporter.domain(" ")).isNull();
    }

    private void given(List<Lead> leads, Company... employers) {
        when(this.source.acceptedLeads(CUSTOMER, PROJECT, LIST)).thenReturn(leads);
        var companies = new java.util.HashMap<UUID, Company>();
        for (var employer : employers) {
            companies.put(employer.id(), employer);
        }
        when(this.source.companies(eq(CUSTOMER), anyCollection())).thenReturn(companies);
    }

    @SuppressWarnings("unchecked")
    private <T> ArgumentCaptor<List<T>> captureList() {
        return ArgumentCaptor.forClass((Class<List<T>>) (Class<?>) List.class);
    }

    private static Lead contactLead(Contact contact) {
        return Lead.builder().id(UUID.randomUUID()).customerId(CUSTOMER).projectId(PROJECT)
                .type(LeadType.LEAD_TYPE_CONTACT).contact(contact).leadListIds(List.of(LIST))
                .createdAt(NOW).updatedAt(NOW).build();
    }

    private static Contact contact(String firstName, String lastName, String email, String phone, UUID companyId) {
        var id = UUID.randomUUID();
        var job = new ContactJob(UUID.randomUUID(), CUSTOMER, id, "CTO", companyId, null, null, NOW, NOW);
        return new Contact(id, CUSTOMER, companyId, firstName, lastName, email, phone, null, null, null, null,
                List.of(job), NOW, NOW, null, null);
    }

    private static Company company(String name, String website) {
        return new Company(UUID.randomUUID(), CUSTOMER, NOW, NOW, null, name, website, null, null,
                new Address(null, null, null, "Paris", "France"), null, null, null, null, null, null, null);
    }
}
