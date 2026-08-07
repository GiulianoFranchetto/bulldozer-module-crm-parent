# External changes required

These changes must be made outside this module's source before the CRM module is fully functional.

## ~~1. `bulldozer-core-lib` — `IntegrationPlatform` enum~~ ✅ Done

## ~~2. `bulldozer-core-lib` — publish updated snapshot~~ ✅ Done

## ~~3. HubSpot OAuth flow in the modulith~~ ✅ Done

`bulldozer-business-private-modulith` now maps `hubspot` in `IntegrationPlatformResolver`, exchanges the code
in `IntegrationOAuthService.finishHubSpot`, and declares the credentials under
`bulldozer.oauth.integrations.hubspot` (and its `bulldozer.projects.oauth.integrations` twin, used by
`ProjectIntegrationTokenRefresher` — HubSpot access tokens live 30 minutes, so the refresh path is not
optional).

**Still required to actually connect a portal:** set `HUBSPOT_CLIENT_ID` and `HUBSPOT_SECRET` in the
modulith's environment. They default to empty so an unset variable does not block startup — but the OAuth
flow will fail with an invalid client until they are provided. The HubSpot app must declare exactly the
scopes configured there:
`oauth crm.objects.contacts.read crm.objects.companies.read crm.objects.deals.read`.

## 4. Database

Create the PostgreSQL databases on the shared RDS instance (`bulldozer-database-dev-2`):
- dev:  `dev_bdz_saas_module_crm`
- prod: `bdz_saas_module_crm`

`ddl-auto: update` creates the tables, and `bulldozer.jobs.init-sql: true` creates the job-library tables, so
nothing else is needed once the databases exist. Note the schema is **not** backward compatible with any
earlier deployment of this module (enum constants renamed, `crm_pipeline_stages` and the link tables now
project-aware) — the tables have never held data, so drop and recreate rather than migrate.

## 5. Salesforce and Pipedrive adapters

`SalesforceSyncService` and `PipedriveSyncService` implement `CrmPlatformSyncService` but throw
`UnsupportedOperationException` (surfaced as `501`). HubSpot is implemented in
`sync/services/hubspot/HubSpotSyncService.java` and is the reference to follow. Neither platform has a usable
official Maven client — use `RestTemplate` against their REST API, as `HubSpotClient` does.

Both also need their own OAuth entry in the modulith (same four places as HubSpot, see item 3).
