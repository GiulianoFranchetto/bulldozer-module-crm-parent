# External changes required

These changes must be made outside this module's source before the CRM module is fully functional.

## 1. Database

Create the prod PostgreSQL database on the shared RDS instance (`bulldozer-database-dev-2`):
`bdz_saas_module_crm`. The dev one, `dev_bdz_saas_module_crm`, exists since 2026-10-09.

`ddl-auto: update` creates the three configuration tables on first start.

## 2. HubSpot app credentials and scopes

The modulith asks for the CRM bridge's scopes on branch `feat/hubspot-crm-write-scopes` (not merged yet):
`oauth crm.objects.contacts.read crm.objects.contacts.write crm.objects.companies.read
crm.objects.companies.write crm.objects.deals.read crm.objects.deals.write crm.schemas.deals.read`, plus
`crm.objects.leads.read crm.objects.leads.write` as **optional** scopes (HubSpot's `optional_scope`): they exist on
Sales Hub Pro / Enterprise only, and HubSpot drops them on other accounts instead of failing the install.
Activities are covered by the contacts scopes.

Still required:
1. Merge the branch and deploy the modulith.
2. The HubSpot app (`~/Documents/bulldozer/hubspot`) declares the same scopes — the leads ones as optional — the
   redirect URLs `https://bulldozer-os.fr/mcp/callback?platform=hubspot` and
   `http://localhost:5600/mcp/callback?platform=hubspot`, and `dev@bulldozer-collective.com` as support email. It is
   uploaded (build #1, 2026-10-12) to the **test account** `Bulldozer_OS_test` (146651817) only.
3. `HUBSPOT_CLIENT_ID` and `HUBSPOT_SECRET` (the test account's app) are in `backend/.keys.env` since 2026-10-12;
   set them in the deployed modulith's environment too.
4. Every project connected before that reconnects HubSpot: its token carries the read-only scopes, and every write
   answers `424` until then.

## 3. HubSpot leads — one check left on a real portal

The leads' `hs_pipeline` / `hs_pipeline_stage` properties, `hs_lead_name` / `hs_lead_type`, and the
`HUBSPOT_DEFINED` / `578` contact association are confirmed by a HubSpot-staff answer showing a lead created
through the API (community thread 122975). Still unconfirmed: the lead pipelines answer at
`GET /crm/pipelines/2026-09/0-136` (`HubSpotContainerKind.LEADS`). Check it on a portal with lead pipelines
(Sales Hub Pro or Enterprise) before relying on lead containers.

## 4. Salesforce

`SalesforceCrmGateway` answers `501`, and `CrmConnectionFactory` refuses Salesforce connections. To implement:
1. Add the Salesforce OAuth flow to the modulith (same places as HubSpot).
2. On `POST /crm/connections`, discover the org's instance URL through Salesforce's OAuth userinfo endpoint with
   the token, and store it in `crm_connections.instance_url` — the modulith's `OAuthToken` does not carry it.
3. A container is a business process — a sales process for opportunities, a lead process for leads — reached
   through `RecordType.BusinessProcessId`; an org without record types needs a default container.
4. Implement `SalesforceCrmGateway` against the REST API reference (no guessed paths), then lift the refusal in
   `CrmConnectionFactory`.
