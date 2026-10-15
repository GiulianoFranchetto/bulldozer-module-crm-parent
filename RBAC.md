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

Every rule below comes from `security/DefaultAccessChecker.java` and the 46 per-endpoint checkers, each of
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
| `DELETE /crm/connections/{connectionId}` | ✓ | ✓ | ✓ | ✓ | Deletes its containers and stages too |
| `GET /crm/{connectionId}/properties` | ✓ | ✓ | ✓ | ✓ | Calls the CRM |
| `POST /crm/{connectionId}/properties` | ✓ | ✓ | ✓ | ✓ | Calls the CRM; changes the portal's schema |
| `POST /crm/{connectionId}/containers` | ✓ | ✓ | ✓ | ✓ | Calls the CRM; imports several containers in one transaction |
| `GET /crm/{connectionId}/containers` | ✓ | ✓ | ✓ | ✓ | Calls the CRM |
| `GET /crm/{connectionId}/containers/{containerId}` | ✓ | ✓ | ✓ | ✓ |  |
| `DELETE /crm/{connectionId}/containers/{containerId}` | ✓ | ✓ | ✓ | ✓ | Deletes its stages too |
| `PUT /crm/{connectionId}/containers/{containerId}/stage-source` | ✓ | ✓ | ✓ | ✓ | Calls the CRM; replaces every stage and mapping |
| `POST /crm/{connectionId}/containers/{containerId}/refresh` | ✓ | ✓ | ✓ | ✓ | Calls the CRM; drops the mappings of vanished stages |
| `GET /crm/{connectionId}/stages` | ✓ | ✓ | ✓ | ✓ |  |
| `PUT /crm/{connectionId}/stages/mapping` | ✓ | ✓ | ✓ | ✓ | Maps several stages in one transaction |
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
| `GET /crm/{connectionId}/leads` | ✓ | ✓ | ✓ | ✓ | Calls the CRM |
| `GET /crm/{connectionId}/leads/{id}` | ✓ | ✓ | ✓ | ✓ | Calls the CRM |
| `POST /crm/{connectionId}/leads` | ✓ | ✓ | ✓ | ✓ | Writes to the CRM |
| `GET /crm/{connectionId}/activities` | ✓ | ✓ | ✓ | ✓ | Calls the CRM |
| `GET /crm/{connectionId}/activities/{id}` | ✓ | ✓ | ✓ | ✓ | Calls the CRM |
| `POST /crm/{connectionId}/activities` | ✓ | ✓ | ✓ | ✓ | Writes to the CRM |
| `PUT /crm/{connectionId}/associations` | ✓ | ✓ | ✓ | ✓ | Writes to the CRM |
| `DELETE /crm/{connectionId}/associations` | ✓ | ✓ | ✓ | ✓ | Writes to the CRM |
| `POST /crm/{connectionId}/lead-list-imports` | ✓ | ✓ | ✓ | ✓ | Reads a lead list of the project (outbound, internal route), writes to the CRM |
| `GET /crm/{connectionId}/landing-pages` | ✓ | ✓ | ✓ | ✓ | Calls the CRM |
| `GET /crm/{connectionId}/landing-pages/{id}` | ✓ | ✓ | ✓ | ✓ | Calls the CRM |
| `POST /crm/{connectionId}/landing-pages` | ✓ | ✓ | ✓ | ✓ | Writes to the CRM |
| `PATCH /crm/{connectionId}/landing-pages/{id}` | ✓ | ✓ | ✓ | ✓ | Writes to the CRM |
| `POST /crm/{connectionId}/landing-pages/{id}/publish` | ✓ | ✓ | ✓ | ✓ | Writes to the CRM; publishes the page |
| `DELETE /crm/{connectionId}/landing-pages/{id}` | ✓ | ✓ | ✓ | ✓ | Deletes the page in the CRM |
| `GET /crm/{connectionId}/forms` | ✓ | ✓ | ✓ | ✓ | Calls the CRM |
| `GET /crm/{connectionId}/forms/{id}` | ✓ | ✓ | ✓ | ✓ | Calls the CRM |
| `POST /crm/{connectionId}/forms` | ✓ | ✓ | ✓ | ✓ | Writes to the CRM |
| `PATCH /crm/{connectionId}/forms/{id}` | ✓ | ✓ | ✓ | ✓ | Writes to the CRM |
| `DELETE /crm/{connectionId}/forms/{id}` | ✓ | ✓ | ✓ | ✓ | Deletes the form in the CRM |

**No endpoint carries `@FeatureNeeded`** — the module has no subscription gate. This is a deliberate, current
decision: the CRM bridge is not on sale separately yet. Revisit when it is.

**Uniform on purpose.** Writes and configuration actions (registering a CRM connection, importing containers and
mapping their stages, creating or updating records in the customer's CRM) are open to any project member, exactly
like the reads — confirmed again for the bridge (V2, 2026-10-09). This was
chosen explicitly rather than inherited: the alternative — restricting writes to the project `MANAGER` — is
the obvious tightening if the module is ever exposed to end customers.

## Global restriction

The module carries `hosting-guard-starter`, so a token minted by a **hosted static site's** public Keycloak
client is additionally checked by `HostingAppTokenFilter`: unless the caller is `BULLDOZER_ADMINISTRATOR`,
`BULLDOZER_OPS` or `BULLDOZER_SALES`, the tenant headers must match the hosting's own tenant and the caller
must hold a membership with that customer. Tokens from the web frontend, the MCP server and service accounts
are unaffected. Off unless `bulldozer.hosting-guard.enabled=true`.
