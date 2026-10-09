# RBAC — CRM module

Access rules of every **authenticated, non-internal** endpoint. The module exposes no `/internal/**` and no
`/pub/**` endpoint.

## Legend

| Column | Meaning |
|---|---|
| **Bdz Admin** | Keycloak role `BULLDOZER_ADMINISTRATOR` (`bulldozer-core-admin`) |
| **Bdz Ops** | Keycloak role `BULLDOZER_OPS` (`bulldozer-core-ops`) |
| **Cust Admin** | `isTargetCustomerAdmin` — administrator of the customer in `X-Bdz-Customer-Id` |
| **Proj Member** | Any membership on the project in `X-Bdz-Project-Id` (a Manager is also a Member) |
| ✓ | Allowed |
| *(empty)* | Denied |

Every rule below comes from `security/DefaultAccessChecker.java` and the 28 per-endpoint checkers, each of
which extends it and adds nothing.

**`BULLDOZER_OPS` is accepted since Bug 67** (2026-08-12). Before that, this module and `tracking` were the
only two whose `DefaultAccessChecker` omitted the ops branch, so an operator who could already read a client
project's ad accounts, Meta analytics and GSC domains got a bare `403` on every CRM endpoint of that same
project. `tracking` was aligned first; this module closes the gap.

## CRM

| Endpoint | Bdz Admin | Bdz Ops | Cust Admin | Proj Member | Notes |
|---|---|---|---|---|---|
| `POST /crm/connections` | ✓ | ✓ | ✓ | ✓ | Checks the modulith holds a token |
| `GET /crm/connections` | ✓ | ✓ | ✓ | ✓ |  |
| `GET /crm/connections/{connectionId}` | ✓ | ✓ | ✓ | ✓ |  |
| `DELETE /crm/connections/{connectionId}` | ✓ | ✓ | ✓ | ✓ | Deletes the stage configuration too |
| `GET /crm/{connectionId}/pipelines` | ✓ | ✓ | ✓ | ✓ | Calls the CRM |
| `GET /crm/{connectionId}/deal-properties` | ✓ | ✓ | ✓ | ✓ | Calls the CRM |
| `GET /crm/{connectionId}/pipelines/{pipelineId}/stage-source` | ✓ | ✓ | ✓ | ✓ |  |
| `PUT /crm/{connectionId}/pipelines/{pipelineId}/stage-source` | ✓ | ✓ | ✓ | ✓ | Calls the CRM; drops the pipeline's mappings when the property changes |
| `GET /crm/{connectionId}/pipelines/{pipelineId}/stage-mappings` | ✓ | ✓ | ✓ | ✓ |  |
| `POST /crm/{connectionId}/pipelines/{pipelineId}/stage-mappings` | ✓ | ✓ | ✓ | ✓ | Calls the CRM |
| `DELETE /crm/{connectionId}/pipelines/{pipelineId}/stage-mappings/{mappingId}` | ✓ | ✓ | ✓ | ✓ |  |
| `GET /crm/{connectionId}/contacts` | ✓ | ✓ | ✓ | ✓ | Calls the CRM |
| `GET /crm/{connectionId}/contacts/{id}` | ✓ | ✓ | ✓ | ✓ | Calls the CRM |
| `POST /crm/{connectionId}/contacts` | ✓ | ✓ | ✓ | ✓ | Writes to the CRM |
| `PATCH /crm/{connectionId}/contacts/{id}` | ✓ | ✓ | ✓ | ✓ | Writes to the CRM |
| `GET /crm/{connectionId}/companies` | ✓ | ✓ | ✓ | ✓ | Calls the CRM |
| `GET /crm/{connectionId}/companies/{id}` | ✓ | ✓ | ✓ | ✓ | Calls the CRM |
| `POST /crm/{connectionId}/companies` | ✓ | ✓ | ✓ | ✓ | Writes to the CRM |
| `PATCH /crm/{connectionId}/companies/{id}` | ✓ | ✓ | ✓ | ✓ | Writes to the CRM |
| `GET /crm/{connectionId}/deals` | ✓ | ✓ | ✓ | ✓ | Calls the CRM |
| `GET /crm/{connectionId}/deals/{id}` | ✓ | ✓ | ✓ | ✓ | Calls the CRM |
| `POST /crm/{connectionId}/deals` | ✓ | ✓ | ✓ | ✓ | Writes to the CRM |
| `PATCH /crm/{connectionId}/deals/{id}` | ✓ | ✓ | ✓ | ✓ | Writes to the CRM |
| `GET /crm/{connectionId}/activities` | ✓ | ✓ | ✓ | ✓ | Calls the CRM |
| `GET /crm/{connectionId}/activities/{id}` | ✓ | ✓ | ✓ | ✓ | Calls the CRM |
| `POST /crm/{connectionId}/activities` | ✓ | ✓ | ✓ | ✓ | Writes to the CRM |
| `PUT /crm/{connectionId}/associations` | ✓ | ✓ | ✓ | ✓ | Writes to the CRM |
| `DELETE /crm/{connectionId}/associations` | ✓ | ✓ | ✓ | ✓ | Writes to the CRM |

**No endpoint carries `@FeatureNeeded`** — the module has no subscription gate. This is a deliberate, current
decision: the CRM bridge is not on sale separately yet. Revisit when it is.

**Uniform on purpose.** Writes and configuration actions (registering a CRM connection, editing the stage
configuration, creating or updating records in the customer's CRM) are open to any project member, exactly
like the reads — confirmed again for the bridge (V2, 2026-10-09). This was
chosen explicitly rather than inherited: the alternative — restricting writes to the project `MANAGER` — is
the obvious tightening if the module is ever exposed to end customers.

## Global restriction

The module carries `hosting-guard-starter`, so a token minted by a **hosted static site's** public Keycloak
client is additionally checked by `HostingAppTokenFilter`: unless the caller is `BULLDOZER_ADMINISTRATOR`,
`BULLDOZER_OPS` or `BULLDOZER_SALES`, the tenant headers must match the hosting's own tenant and the caller
must hold a membership with that customer. Tokens from the web frontend, the MCP server and service accounts
are unaffected. Off unless `bulldozer.hosting-guard.enabled=true`.
