# External changes required

These changes must be made outside this module's source before the CRM module is fully functional.

## 1. Database

Create the prod PostgreSQL database on the shared RDS instance (`bulldozer-database-dev-2`):
`bdz_saas_module_crm`. The dev one, `dev_bdz_saas_module_crm`, exists since 2026-10-09.

`ddl-auto: update` creates the three configuration tables on first start.

## 2. HubSpot app credentials in the modulith

`HUBSPOT_CLIENT_ID` and `HUBSPOT_SECRET` must be set in the modulith's environment. The bridge now **writes**, so
the HubSpot app and `bulldozer.oauth.integrations.hubspot` (and its `bulldozer.projects.oauth.integrations` twin)
must declare the write scopes as well as the read ones:
`oauth crm.objects.contacts.read crm.objects.contacts.write crm.objects.companies.read
crm.objects.companies.write crm.objects.deals.read crm.objects.deals.write crm.objects.leads.read
crm.objects.leads.write`, plus `crm.schemas.deals.read` (and its leads twin) for the properties. Check the
exact scope names in HubSpot's scope reference before changing them. A project connected with the read-only
scopes answers `424` on every write until it reconnects.

## 3. HubSpot leads — confirm on a real portal

HubSpot's official docs give the leads' object type id (`0-136`) but not their pipeline and stage properties.
`HubSpotContainerKind.LEADS` assumes `hs_pipeline` / `hs_pipeline_stage`, the convention the docs give for
tickets, projects and services. Before relying on lead containers, check on a portal with lead pipelines (Sales
Hub Pro or Enterprise):
1. `GET /crm/properties/2026-09/leads` lists `hs_pipeline` and `hs_pipeline_stage`.
2. `GET /crm/pipelines/2026-09/0-136` answers the lead pipelines.
3. Creating a lead with the `HUBSPOT_DEFINED` / `578` contact association succeeds.

## 4. Salesforce

`SalesforceCrmGateway` answers `501`, and `CrmConnectionFactory` refuses Salesforce connections. To implement:
1. Add the Salesforce OAuth flow to the modulith (same places as HubSpot).
2. On `POST /crm/connections`, discover the org's instance URL through Salesforce's OAuth userinfo endpoint with
   the token, and store it in `crm_connections.instance_url` — the modulith's `OAuthToken` does not carry it.
3. A container is a business process — a sales process for opportunities, a lead process for leads — reached
   through `RecordType.BusinessProcessId`; an org without record types needs a default container.
4. Implement `SalesforceCrmGateway` against the REST API reference (no guessed paths), then lift the refusal in
   `CrmConnectionFactory`.
