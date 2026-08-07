# CRM API

CRM data unification. The module **ingests read-only** from a customer's CRM (HubSpot today; Salesforce and
Pipedrive are declared but not implemented) and exposes contacts, companies, deals, activities and pipelines
under one Bulldozer-side model, so features can work CRM-agnostic.

Two ideas drive the contract:

- **Mirror + link.** Every synced object is stored as a mirror row keyed on `(customerId, projectId, platform,
  sourceId)` — `sourceId` being the CRM's own id, which is the dedup key. Contacts and companies additionally
  carry `contactId` / `companyId`, the id of the **canonical Bulldozer contact / company** (contacts and
  companies modules) they were resolved to. Those are null while a record has too little identity to resolve
  (a contact with no name, a company with no domain).
- **Mandatory stage mapping.** Bulldozer defines a fixed `CrmDealStage`. Every pipeline stage the CRM reports
  must be mapped to one, or the sync refuses to run (`400`) rather than silently dropping deals.

Base URL: this module's service (port `43200` in dev).

## Conventions

| | |
|---|---|
| Auth | `Authorization: Bearer <JWT>` on every endpoint |
| Tenant | `X-Bdz-Customer-Id` and `X-Bdz-Project-Id` headers, both required |
| Subscription | **No `@FeatureNeeded` on any endpoint** — the module is not gated yet |
| Errors | Every 4xx carries `{"error": "<message>"}`; 5xx is bodyless |
| Pagination | `page` (0-based, default `0`) and `size` (default `20`, max `200`) query parameters; both optional. List endpoints return a `ResponsePage<T>` |
| MCP | Not exposed — there is no `mcp_tools.do` in this project |

**RBAC — identical for all 15 endpoints**: `BULLDOZER_ADMINISTRATOR`, the target customer's administrator, or
**any member of the project**. Reads and writes alike; there is no manager-only endpoint. See `RBAC.md`.

---

## Contacts

### GET `/crm/contacts`
Lists mirrored CRM contacts.

| Query param | Type | Required | Description |
|---|---|---|---|
| `q` | `string` | No | Case-insensitive match on first name, last name or email |
| `platform` | `IntegrationPlatform` | No | Restrict to one CRM |
| `page` / `size` | `int` | No | Defaults to page 0, size 20; `size` is capped at 200 |

**200** → `ResponsePage<CrmContact>`.

### GET `/crm/contacts/{id}`
**200** → `GetCrmContactResponse { contact: CrmContact }`. **404** if the id is unknown in this project. **400** if `id` is not a UUID.

## Companies

### GET `/crm/companies`
Same shape as `/crm/contacts`; `q` matches name or domain. **200** → `ResponsePage<CrmCompany>`.

### GET `/crm/companies/{id}`
**200** → `GetCrmCompanyResponse { company: CrmCompany }`. **404** / **400** as above.

## Deals

### GET `/crm/deals`

| Query param | Type | Required | Description |
|---|---|---|---|
| `internalStage` | `CrmDealStage` | No | Filter on the Bulldozer stage, not the CRM's |
| `pipelineId` | `UUID` | No | Bulldozer pipeline id (not the CRM's `sourceId`) |
| `platform` | `IntegrationPlatform` | No | |
| `page` / `size` | `int` | No | Defaults to page 0, size 20; `size` is capped at 200 |

**200** → `ResponsePage<CrmDeal>`.

### GET `/crm/deals/{id}`
**200** → `GetCrmDealResponse { deal: CrmDeal }`.

## Activities

### GET `/crm/activities`

| Query param | Type | Required | Description |
|---|---|---|---|
| `type` | `CrmActivityType` | No | |
| `dealId` / `contactId` / `companyId` | `UUID` | No | Bulldozer-side CRM mirror ids |
| `platform` | `IntegrationPlatform` | No | |
| `page` / `size` | `int` | No | Defaults to page 0, size 20; `size` is capped at 200 |

**200** → `ResponsePage<CrmActivity>`.

### GET `/crm/activities/{id}`
**200** → `GetCrmActivityResponse { activity: CrmActivity }`.

---

## Pipelines and stage mappings

### GET `/crm/pipelines`
Lists the known pipelines with their stages and, per stage, the mapping to a `CrmDealStage` when one exists.

| Query param | Type | Required |
|---|---|---|
| `platform` | `IntegrationPlatform` | No |

**200** → `ListCrmPipelinesResponse { pipelines: CrmPipeline[] }`.

### POST `/crm/pipelines/discover`
Calls the CRM, upserts its pipelines and stages, soft-deletes the stages it no longer returns, and returns the
result. Run this before configuring mappings.

Body: `{ "platform": "INTEGRATION_PLATFORM_HUBSPOT" }`

**200** → `ListCrmPipelinesResponse`.
**424** if the project has no OAuth token for that platform.
**501** for Salesforce / Pipedrive.

### POST `/crm/stage-mappings`
Maps one CRM pipeline stage to a Bulldozer `CrmDealStage`.

Body:

| Field | Type | Required | Description |
|---|---|---|---|
| `pipelineStageId` | `UUID` | Yes | Bulldozer stage id, from `GET /crm/pipelines` |
| `internalStage` | `CrmDealStage` | Yes | |

**201**, no body.
**400** if `pipelineStageId` is not a stage of this project (a stage belonging to another customer is rejected
here, not silently accepted).
**409** if the stage already has a mapping.

### DELETE `/crm/stage-mappings/{mappingId}`
**204**, no body. **400** if the mapping does not belong to this project.

---

## Connections

### POST `/crm/connections`
Registers that this project uses a CRM. A project may connect several CRMs at once. The OAuth token itself is
**not** stored here — it lives in the projects module and is obtained through
`POST /integrations/hubspot/oauth/start` (see `INTEGRATIONS_API.md`).

Body: `{ "platform": "INTEGRATION_PLATFORM_HUBSPOT" }`

**201**, no body. **409** if the project already has a connection for that platform.

---

## Sync

The sync is asynchronous and job-backed. `POST /crm/sync/trigger` only enqueues: it writes a run row and a
job, then returns. A poller runs the job as four chained steps — pipelines, objects, associations, canonical
linking — one step per tick (5 min), so a full sync completes within ~20 minutes of being triggered. The linking
step works in batches of 200 rows and reschedules itself while it is making progress, so a large portal takes
additional ticks. A scheduler enqueues one run per active connection at 06:00 and 18:00 UTC.

Calls to HubSpot are throttled and a `429` or transient `5xx` is retried with backoff (honouring HubSpot's
`Retry-After`), so a rate limit slows a sync down instead of failing it.

### POST `/crm/sync/trigger`
Body: `{ "platform": "INTEGRATION_PLATFORM_HUBSPOT" }`

**200** → `CrmSyncRunStartedResponse { syncRunId }`. Poll `GET /crm/sync/runs` for the outcome.
**400** if `platform` is not a CRM platform.
**409** if a sync of that platform is already in flight for this project — N triggers do not queue N syncs. A run
left behind by a crashed service does not block: the check reads the job's state, not the run's status.

Note: the run is created with status `CRM_SYNC_STATUS_RUNNING` even before the poller picks it up, and a
failure surfaces on the run row — not on this response.

### GET `/crm/sync/runs`

| Query param | Type | Required |
|---|---|---|
| `platform` | `IntegrationPlatform` | No |
| `status` | `CrmSyncStatus` | No |
| `page` / `size` | `int` | No — defaults to page 0, size 20; `size` is capped at 200 |

**200** → `ResponsePage<CrmSyncRun>`, newest first.

Failure modes visible on a run:
- unmapped pipeline stages → `CRM_SYNC_STATUS_FAILED`, `errorMessage` listing every `[pipeline] stage`;
- no OAuth token → `CRM_SYNC_STATUS_FAILED`;
- objects synced but the canonical linking step failed → `CRM_SYNC_STATUS_PARTIAL`, mirror data usable;
- run stranded over 6h (service killed mid-sync) → closed as `CRM_SYNC_STATUS_FAILED` by a sweeper.

---

## Data models

### `CrmContact`

| Field | Type | Nullable | Description |
|---|---|---|---|
| `id` | `UUID` | No | Bulldozer id of the mirror row |
| `customerId` / `projectId` | `UUID` | No | |
| `platform` | `IntegrationPlatform` | No | |
| `sourceId` | `string` | No | The CRM's own id — the dedup key |
| `contactId` | `UUID` | **Yes** | The canonical Bulldozer contact; null while unlinked |
| `firstName`, `lastName`, `email`, `phone`, `jobTitle`, `linkedInUrl` | `string` | Yes | As reported by the CRM |
| `createdAt` | `Instant` | No | |

### `CrmCompany`

| Field | Type | Nullable | Description |
|---|---|---|---|
| `id`, `customerId`, `projectId`, `platform`, `sourceId` | | No | As above |
| `companyId` | `UUID` | **Yes** | The canonical Bulldozer company; null while unlinked |
| `name` | `string` | No | |
| `domain`, `industry`, `country`, `city` | `string` | Yes | |
| `createdAt` | `Instant` | No | |

### `CrmDeal`

| Field | Type | Nullable | Description |
|---|---|---|---|
| `id`, `customerId`, `projectId`, `platform`, `sourceId` | | No | |
| `name` | `string` | No | |
| `amount` | `BigDecimal` | Yes | |
| `currency` | `string` (3) | Yes | |
| `closeDate` | `Instant` | Yes | |
| `internalStage` | `CrmDealStage` | No | Never null: a deal whose stage has no mapping is not stored |
| `pipelineId`, `pipelineStageId` | `UUID` | No | Bulldozer ids |
| `createdAt` | `Instant` | No | |

### `CrmActivity`

| Field | Type | Nullable |
|---|---|---|
| `id`, `customerId`, `projectId`, `platform`, `sourceId` | | No |
| `type` | `CrmActivityType` | No |
| `title`, `body` | `string` | Yes |
| `dueDate`, `completedAt` | `Instant` | Yes |
| `dealId`, `contactId`, `companyId` | `UUID` | Yes |
| `createdAt` | `Instant` | No |

### `CrmPipeline`

| Field | Type | Nullable |
|---|---|---|
| `id`, `customerId`, `projectId`, `platform`, `sourceId` | | No |
| `name` | `string` | No |
| `stages` | `CrmPipelineStage[]` | No |

### `CrmPipelineStage`

| Field | Type | Nullable | Description |
|---|---|---|---|
| `id` | `UUID` | No | Pass this as `pipelineStageId` when creating a mapping |
| `sourceId` | `string` | No | The CRM's own stage id |
| `label` | `string` | No | |
| `displayOrder` | `int` | No | |
| `mappedInternalStage` | `CrmDealStage` | Yes | Null = not mapped = the sync will refuse to run |
| `mappingId` | `UUID` | Yes | Pass this to `DELETE /crm/stage-mappings/{mappingId}` |

### `CrmSyncRun`

| Field | Type | Nullable |
|---|---|---|
| `id`, `customerId`, `projectId`, `platform` | | No |
| `startedAt` | `Instant` | No |
| `finishedAt` | `Instant` | Yes |
| `status` | `CrmSyncStatus` | No |
| `contactsSynced`, `companiesSynced`, `dealsSynced`, `activitiesSynced` | `int` | No |
| `errorMessage` | `string` | Yes |

### Enums

`CrmDealStage` — `CRM_DEAL_STAGE_PROSPECTING`, `CRM_DEAL_STAGE_QUALIFIED`, `CRM_DEAL_STAGE_PROPOSAL`,
`CRM_DEAL_STAGE_NEGOTIATION`, `CRM_DEAL_STAGE_WON`, `CRM_DEAL_STAGE_LOST`

`CrmActivityType` — `CRM_ACTIVITY_TYPE_CALL`, `CRM_ACTIVITY_TYPE_EMAIL`, `CRM_ACTIVITY_TYPE_MEETING`,
`CRM_ACTIVITY_TYPE_NOTE`, `CRM_ACTIVITY_TYPE_TASK`

`CrmSyncStatus` — `CRM_SYNC_STATUS_RUNNING`, `CRM_SYNC_STATUS_SUCCESS`, `CRM_SYNC_STATUS_PARTIAL`,
`CRM_SYNC_STATUS_FAILED`

`IntegrationPlatform` — from `core-lib`. Only `INTEGRATION_PLATFORM_HUBSPOT`,
`INTEGRATION_PLATFORM_SALESFORCE` and `INTEGRATION_PLATFORM_PIPEDRIVE` are accepted; anything else is a
`400`. Only HubSpot is implemented — the other two answer `501`.

---

## Error codes

| Code | When |
|---|---|
| 400 | Malformed UUID or enum, missing mandatory parameter, unmapped pipeline stages, non-CRM platform, stage/mapping id outside the project |
| 403 | The caller is not a member of the project (body: `{"error": "Forbidden"}`) |
| 404 | Unknown id in this project |
| 409 | Connection already registered, stage already mapped, or a sync of that platform is already in flight |
| 424 | No OAuth token stored for that CRM platform — run the OAuth flow first |
| 501 | Salesforce / Pipedrive adapters not implemented |
| 502 | The CRM or an internal module answered an error |
| 500 | Internal failure (bodyless) |

---

## Onboarding a project

1. `POST /integrations/hubspot/oauth/start` (projects module) and complete the OAuth flow → the project gets
   an `INTEGRATION_PLATFORM_HUBSPOT` token.
2. `POST /crm/connections` with that platform.
3. `POST /crm/pipelines/discover`.
4. `POST /crm/stage-mappings` for **every** stage returned with `mappedInternalStage: null`.
5. `POST /crm/sync/trigger`, then poll `GET /crm/sync/runs`.
