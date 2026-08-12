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

Every rule below comes from `security/DefaultAccessChecker.java` and the 15 per-endpoint checkers, each of
which extends it and adds nothing.

**`BULLDOZER_OPS` is accepted since Bug 67** (2026-08-12). Before that, this module and `tracking` were the
only two whose `DefaultAccessChecker` omitted the ops branch, so an operator who could already read a client
project's ad accounts, Meta analytics and GSC domains got a bare `403` on every CRM endpoint of that same
project. `tracking` was aligned first; this module closes the gap.

## CRM

| Endpoint | Bdz Admin | Bdz Ops | Cust Admin | Proj Member | Notes |
|---|---|---|---|---|---|
| `GET /crm/contacts` | ✓ | ✓ | ✓ | ✓ | |
| `GET /crm/contacts/{id}` | ✓ | ✓ | ✓ | ✓ | |
| `GET /crm/companies` | ✓ | ✓ | ✓ | ✓ | |
| `GET /crm/companies/{id}` | ✓ | ✓ | ✓ | ✓ | |
| `GET /crm/deals` | ✓ | ✓ | ✓ | ✓ | |
| `GET /crm/deals/{id}` | ✓ | ✓ | ✓ | ✓ | |
| `GET /crm/activities` | ✓ | ✓ | ✓ | ✓ | |
| `GET /crm/activities/{id}` | ✓ | ✓ | ✓ | ✓ | |
| `GET /crm/pipelines` | ✓ | ✓ | ✓ | ✓ | |
| `POST /crm/pipelines/discover` | ✓ | ✓ | ✓ | ✓ | Calls the CRM API |
| `POST /crm/stage-mappings` | ✓ | ✓ | ✓ | ✓ | |
| `DELETE /crm/stage-mappings/{mappingId}` | ✓ | ✓ | ✓ | ✓ | |
| `POST /crm/connections` | ✓ | ✓ | ✓ | ✓ | Tenant configuration, still open to any member |
| `POST /crm/sync/trigger` | ✓ | ✓ | ✓ | ✓ | |
| `GET /crm/sync/runs` | ✓ | ✓ | ✓ | ✓ | |

**No endpoint carries `@FeatureNeeded`** — the module has no subscription gate. This is a deliberate, current
decision: the CRM unification is not on sale separately yet. Revisit when it is.

**Uniform on purpose.** Writes and configuration actions (registering a CRM connection, discovering pipelines,
editing stage mappings, triggering a sync) are open to any project member, exactly like the reads. This was
chosen explicitly rather than inherited: the alternative — restricting writes to the project `MANAGER` — is
the obvious tightening if the module is ever exposed to end customers.

## Global restriction

The module carries `hosting-guard-starter`, so a token minted by a **hosted static site's** public Keycloak
client is additionally checked by `HostingAppTokenFilter`: unless the caller is `BULLDOZER_ADMINISTRATOR`,
`BULLDOZER_OPS` or `BULLDOZER_SALES`, the tenant headers must match the hosting's own tenant and the caller
must hold a membership with that customer. Tokens from the web frontend, the MCP server and service accounts
are unaffected. Off unless `bulldozer.hosting-guard.enabled=true`.
