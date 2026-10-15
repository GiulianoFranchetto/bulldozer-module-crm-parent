# External changes required

These changes must be made outside this module's source before the CRM module is fully functional.

## 1. Database

Create the prod PostgreSQL database on the shared RDS instance (`bulldozer-database-dev-2`):
`bdz_saas_module_crm`. The dev one, `dev_bdz_saas_module_crm`, exists since 2026-10-09.

`ddl-auto: update` creates the three configuration tables on first start.

## 2. HubSpot app credentials and scopes

The read scopes are required: `oauth crm.objects.contacts.read crm.objects.companies.read crm.objects.deals.read
crm.schemas.deals.read`. Every write scope is **optional**, and asked for only when the customer agrees to let
Bulldozer modify their CRM (`allowWrite` on the modulith's OAuth start, see `INTEGRATIONS_API.md`):
`crm.objects.contacts.write crm.objects.companies.write crm.objects.deals.write crm.objects.leads.write`.
`crm.objects.leads.read` is optional too: it exists on Sales Hub Pro / Enterprise only, and HubSpot drops it on
other accounts instead of failing the install. Activities are covered by the contacts scopes.

Done: the modulith side is merged in `master` (`7b7065e8`, `0cad7d84`) and deployed with `HUBSPOT_CLIENT_ID` /
`HUBSPOT_SECRET`; the HubSpot app (`~/Documents/bulldozer/hubspot`, write scopes as `optionalScopes`) is uploaded as
build #2 (2026-10-13) to the **test account** `Bulldozer_OS_test` (146651817), its Acceptable Use Policy signed. A
read-only connection and a write connection were both tested end to end on that portal (2026-10-13).

Added on 2026-10-15, not yet live: the schema write scopes (`crm.schemas.<type>.write` for contacts, companies and deals —
creating a property; HubSpot refuses those of calls, emails, meetings, notes and tasks to public apps) in `write-scopes`, and `content` (landing pages, Content
Hub / Marketing Hub Pro only) and `forms` in `optional-scopes`: both cover reading and writing, so they are asked even
without `allowWrite`. The modulith's `application.yaml` and the app's `app-hsmeta.json` carry them (uncommitted). The
leads' schema scope is not in HubSpot's create-property reference: not asked.
1. Commit and deploy the modulith.
2. Done: build #5 deployed to the test account (2026-10-15). Reconnect the test portal.
3. On that portal, create a contact `SELECT` property, find it with `GET …/properties?type=CRM_OBJECT_TYPE_CONTACT&name=…`,
   then create it again: HubSpot's duplicate answer is assumed to be a `409`, not yet seen.
4. Create a property on a note (`CRM_OBJECT_TYPE_NOTE`): HubSpot's reference lists its schema scopes as alternatives,
   so `crm.schemas.contacts.write` may be enough; otherwise it answers `424`.

Keep in mind: a portal connected read-only keeps the write scopes locked — HubSpot records them as disabled for the
app, and a portal admin must re-enable them in the app's admin settings (link in the consent screen) before
reconnecting.

## 3. HubSpot leads — one check left on a real portal

The leads' `hs_pipeline` / `hs_pipeline_stage` properties, `hs_lead_name` / `hs_lead_type`, and the
`HUBSPOT_DEFINED` / `578` contact association are confirmed by a HubSpot-staff answer showing a lead created
through the API (community thread 122975). Still unconfirmed: the lead pipelines answer at
`GET /crm/pipelines/2026-09/0-136` (`HubSpotContainerKind.LEADS`). Check it on a portal with lead pipelines
(Sales Hub Pro or Enterprise) before relying on lead containers: the test portal lists no lead pipeline, even with
the leads scopes granted (2026-10-13).

## 4. Lead list import

`POST /crm/{connectionId}/lead-list-imports` reads the list's leads through the outbound module's new internal route
`GET /internal/outbound/lead-lists/{id}/leads` (`InternalListLeadListLeadsClient`), committed on the outbound branch
`lead-list-v2` (`0ec39b6`), which also carries the company leads. Until that branch is merged and deployed, every
import fails on reading the leads. Still to check on a portal: the note → company association type `190`
(`HubSpotCrmGateway.NOTE_TO_COMPANY_ASSOCIATION_TYPE`; note → contact `202` is in HubSpot's notes guide), and the
plural object names in the associations batch paths.

## 5. Landing pages and forms — to check on a portal

The bodies sent to HubSpot are minimal: its specs mark nearly every field of a page and of a form as required
(`configuration`, `displayOptions` and its `style`, a phone field's `validation`…), which a real create surely does not
need. On the test portal (forms work on any tier; landing pages need Content Hub or Marketing Hub Pro):
1. Create a draft landing page on a template of the portal's theme, read it, update its `htmlTitle`, publish it, then
   delete it.
2. Create a form with an email field, a `SELECT` field and `CRM_FORM_LEGAL_CONSENT_TYPE_NONE`, read it back, replace
   its fields, then delete it. Also a phone field: no `validation` is sent.
3. Check that a `PATCH` of a form's `configuration` (`language` only) keeps its `postSubmitAction`: HubSpot may replace
   the whole nested object.
4. Report every field HubSpot refuses here, and complete `HubSpotLandingPageMapper` / `HubSpotFormMapper` with the
   documented defaults only.

The forms API is a beta (`/marketing/forms/2027-03-beta`): follow HubSpot's changelog, and move to the first stable
dated version once out.

## 6. Salesforce

`SalesforceCrmGateway` answers `501`, and `CrmConnectionFactory` refuses Salesforce connections. To implement:
1. Add the Salesforce OAuth flow to the modulith (same places as HubSpot).
2. On `POST /crm/connections`, discover the org's instance URL through Salesforce's OAuth userinfo endpoint with
   the token, and store it in `crm_connections.instance_url` — the modulith's `OAuthToken` does not carry it.
3. A container is a business process — a sales process for opportunities, a lead process for leads — reached
   through `RecordType.BusinessProcessId`; an org without record types needs a default container.
4. Implement `SalesforceCrmGateway` against the REST API reference (no guessed paths), then lift the refusal in
   `CrmConnectionFactory`.
5. Landing pages and forms stay `501` even then: on Salesforce they live in Account Engagement (Pardot), a separate
   product with its own API and a `Pardot-Business-Unit-Id`. Its v5 reads landing pages, and reads and creates forms —
   no update or delete documented for either.

## 7. Frontend

Found while testing on the HubSpot test portal (2026-10-13):
1. Reconnecting HubSpot must only rerun the OAuth flow: deleting and re-registering the CRM connection deletes its
   imported pipelines and their stage mapping.
2. `GET …/properties` takes a `CrmObjectType` (`type=CRM_OBJECT_TYPE_DEAL`, no longer `CRM_CONTAINER_TYPE_DEAL`), and
   the filters `name` / `selectable`: the stage source select uses `selectable=true`.
3. An activity's `body` comes as raw HTML from the CRM: sanitize it before rendering.
4. Contact creation offers `legalBasis` — the options of `hs_legal_basis` (`GET …/properties?type=CRM_OBJECT_TYPE_CONTACT&name=hs_legal_basis`),
   field hidden when the portal has no such property. Not yet checked: what a portal without GDPR features answers
   there.
5. Property creation (`POST …/properties`): the group select lists the distinct `groupName` of
   `GET …/properties?type=…`; options are asked for only with `CRM_PROPERTY_FIELD_TYPE_SELECT`.
6. Landing pages and forms: a page's `content` is the CRM's own format (HubSpot `layoutSections` / `widgets`) — edit
   it in HubSpot, or pass it through untouched; a form field's `name` picks from `GET …/properties`
   (`CRM_OBJECT_TYPE_CONTACT` / `CRM_OBJECT_TYPE_COMPANY`); show `CRM_FORM_FIELD_TYPE_OTHER` fields read only, and
   disable replacing the fields of a form holding one.
