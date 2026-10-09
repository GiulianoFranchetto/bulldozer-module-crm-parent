# External changes required

These changes must be made outside this module's source before the CRM module is fully functional.

## 1. Database

Create the PostgreSQL databases on the shared RDS instance (`bulldozer-database-dev-2`):
- dev:  `dev_bdz_saas_module_crm`
- prod: `bdz_saas_module_crm`

`ddl-auto: update` creates the three configuration tables. If a dev database was created for the V1 (the
mirror strategy), drop it and recreate it: the V1 tables (`crm_contacts`, `crm_deals`, the job-library tables…)
are dead and nothing migrates them.

## 2. HubSpot app credentials in the modulith

`HUBSPOT_CLIENT_ID` and `HUBSPOT_SECRET` must be set in the modulith's environment. The bridge now **writes**, so
the HubSpot app and `bulldozer.oauth.integrations.hubspot` (and its `bulldozer.projects.oauth.integrations` twin)
must declare the write scopes as well as the read ones:
`oauth crm.objects.contacts.read crm.objects.contacts.write crm.objects.companies.read
crm.objects.companies.write crm.objects.deals.read crm.objects.deals.write`, plus `crm.schemas.deals.read` for
the deal properties. Check the exact scope names in HubSpot's scope reference before changing them. A project
connected with the read-only scopes answers `424` on every write until it reconnects.

## 3. Salesforce

`SalesforceCrmGateway` answers `501`, and `CrmConnectionFactory` refuses Salesforce connections. To implement:
1. Add the Salesforce OAuth flow to the modulith (same places as HubSpot).
2. On `POST /crm/connections`, discover the org's instance URL through Salesforce's OAuth userinfo endpoint with
   the token, and store it in `crm_connections.instance_url` — the modulith's `OAuthToken` does not carry it.
3. Implement `SalesforceCrmGateway` against the REST API reference (no guessed paths), then lift the refusal in
   `CrmConnectionFactory`.
