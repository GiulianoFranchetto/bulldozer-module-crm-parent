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

Done: the modulith side is merged in `master` (`7b7065e8`, `0cad7d84`); the HubSpot app
(`~/Documents/bulldozer/hubspot`, write scopes as `optionalScopes`) is uploaded as build #2 (2026-10-13) to the
**test account** `Bulldozer_OS_test` (146651817), its Acceptable Use Policy signed. A read-only connection and a
write connection were both tested end to end on that portal (2026-10-13).

Still required:
1. Deploy the modulith from `master`, with `HUBSPOT_CLIENT_ID` and `HUBSPOT_SECRET` (the test account's app, in
   `backend/.keys.env` since 2026-10-12) in its environment.
2. A portal connected read-only keeps the write scopes locked: HubSpot records them as disabled for the app, and a
   portal admin must re-enable them in the app's admin settings (link in the consent screen) before reconnecting.

## 3. HubSpot leads — one check left on a real portal

The leads' `hs_pipeline` / `hs_pipeline_stage` properties, `hs_lead_name` / `hs_lead_type`, and the
`HUBSPOT_DEFINED` / `578` contact association are confirmed by a HubSpot-staff answer showing a lead created
through the API (community thread 122975). Still unconfirmed: the lead pipelines answer at
`GET /crm/pipelines/2026-09/0-136` (`HubSpotContainerKind.LEADS`). Check it on a portal with lead pipelines
(Sales Hub Pro or Enterprise) before relying on lead containers: the test portal lists no lead pipeline, even with
the leads scopes granted (2026-10-13).

## 4. Salesforce

`SalesforceCrmGateway` answers `501`, and `CrmConnectionFactory` refuses Salesforce connections. To implement:
1. Add the Salesforce OAuth flow to the modulith (same places as HubSpot).
2. On `POST /crm/connections`, discover the org's instance URL through Salesforce's OAuth userinfo endpoint with
   the token, and store it in `crm_connections.instance_url` — the modulith's `OAuthToken` does not carry it.
3. A container is a business process — a sales process for opportunities, a lead process for leads — reached
   through `RecordType.BusinessProcessId`; an org without record types needs a default container.
4. Implement `SalesforceCrmGateway` against the REST API reference (no guessed paths), then lift the refusal in
   `CrmConnectionFactory`.

## 5. Frontend

Found while testing on the HubSpot test portal (2026-10-13):
1. Reconnecting HubSpot must only rerun the OAuth flow: deleting and re-registering the CRM connection deletes its
   imported pipelines and their stage mapping.
2. `GET …/properties` takes a `CrmObjectType` (`type=CRM_OBJECT_TYPE_DEAL`, no longer `CRM_CONTAINER_TYPE_DEAL`), and
   the filters `name` / `selectable`: the stage source select uses `selectable=true`.
3. An activity's `body` comes as raw HTML from the CRM: sanitize it before rendering.
4. Contact creation offers `legalBasis` — the options of `hs_legal_basis` (`GET …/properties?type=CRM_OBJECT_TYPE_CONTACT&name=hs_legal_basis`),
   field hidden when the portal has no such property. Not yet checked: what a portal without GDPR features answers
   there.
