# External changes required

These changes must be made in other repositories before the CRM module is fully functional.

## ~~1. `bulldozer-core-lib` — `IntegrationPlatform` enum~~ ✅ Done

## ~~2. `bulldozer-core-lib` — publish updated snapshot~~ ✅ Done

## 3. CRM client libraries — choose and add to `module-crm/pom.xml`

The platform sync services (`HubSpotSyncService`, `SalesforceSyncService`, `PipedriveSyncService`)
are stubs. Add the chosen client library dependency and implement each service:

| Platform   | Recommended library                          | Maven coordinates (example)                                      |
|------------|----------------------------------------------|------------------------------------------------------------------|
| HubSpot    | HubSpot Java API Client                      | `com.hubspot.crm.client:hubspot-api-client:...`                  |
| Salesforce | Force.com REST API (or simple RestTemplate)  | No official Maven artifact — use Spring RestTemplate + REST API  |
| PipeDrive  | PipeDrive Java client or RestTemplate        | No official Maven artifact — use Spring RestTemplate + REST API  |

## 4. Database

Create the PostgreSQL database for this module:
- dev:  `dev_bdz_saas_module_crm`
- prod: `bdz_saas_module_crm`

on the same RDS instance used by other modules.
