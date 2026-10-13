# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

# Project definition
This project is a part of the Bulldozer SaaS eco-system.
It must respect all Bulldozer coding style, defined in the global BULLDOZER_BACKEND.md

# Commands
Snapshot dependencies (core-lib, security, logging) come from the local repository: build offline with `-o`.

```bash
mvn -q -o verify                                     # build + every test, from the root
mvn -o -pl module-crm -am test -Dtest=HubSpotMapperTest -Dsurefire.failIfNoSpecifiedTests=false  # one test class
DB_PASSWORD=… mvn -o -pl module-crm spring-boot:run -Dspring-boot.run.profiles=dev   # port 43200, dev DB
./push_to_server.sh                                  # scp the built jar to the EC2 host
```

There is no `@SpringBootTest`: a missing bean only shows when the context starts, which needs `DB_PASSWORD` and a
profile (`dev` / `prod` set the datasource).

# Modules
- `crm-api` — the HTTP contract (requests, responses, `*Param` query records, enums), shared with consumers. Never
  passed to a service: controllers map it onto `*Builder` records.
- `module-crm` — one package per domain (`connections`, `containers`, `contacts`, `companies`, `deals`, `leads`,
  `activities`, `associations`, `imports`), each split into `controllers` / `security` / `services`, plus `gateway`.

# Access control
`security/DefaultAccessChecker` grants access to `BULLDOZER_ADMINISTRATOR`, `BULLDOZER_OPS`, the target
customer's administrator, then any project membership — aligned on the rest of the fleet since Bug 67. The 34
per-endpoint checkers are pure `super.checkAccess(context)` pass-throughs, and no endpoint carries
`@FeatureNeeded`. Full table in `RBAC.md`, to keep in sync whenever an endpoint or a checker changes.

# Architecture
The module is a **bridge**: every request is relayed to the customer's CRM, and **no CRM data is ever stored**
— only configuration (`crm_connections`, `crm_containers`, `crm_container_stages`). Deals and leads are only
reached through a **container** (an imported CRM pipeline, `containerId` required). Each CRM sits behind
`gateway/CrmGateway`; `HubSpotCrmGateway` targets HubSpot's date-versioned API (`2026-09`), never `/crm/v3`.
No CRM call may run inside a `@Transactional` method.

- **Tokens.** The OAuth token lives in the modulith, fetched per request (`CrmSessionOpener` →
  `PlatformTokenFetcher`). A connection row holds no token: reconnecting a CRM never needs a new connection. The
  write scopes are optional — a read-only connection is normal, and a write then answers 424.
- **HubSpot calls.** `HubSpotClientFactory.create(session)` → `HubSpotClient` → `HubSpotRequestExecutor`, which
  spaces calls per connection and retries a 429 always, a 5xx or timeout only on an idempotent call: a create
  (`POST`) is never replayed. `HubSpotClient.translate` maps HubSpot's 4xx onto the module's exceptions;
  `configurations/GlobalControllerAdviser` turns those into the contract's codes (401/403 → 424, unusable answer →
  502).
- **Stages.** A container's stage source is a property (native `dealstage` / `hs_pipeline_stage`, or a custom
  enumerated, writable one); its values are the container's stages, each mapped to a `CrmDealStage` /
  `CrmLeadStage`. `CrmStageSources` reads and validates a source; `CrmStageTable` translates both ways. Creating in
  a custom-source container also needs the caller's `pipelineStage`.
- **No delete** of CRM records: only configuration and associations can be removed.

# Contract
Any change to an endpoint's contract also updates `~/Documents/bulldozer/ai/bulldozer-api-docs/CRM_API.md` and,
since `mcp_tools.do` is present, `CrmTools` in `bulldozer-mcp-server`. `TODO.md` lists what must happen outside
this repo (prod database, modulith, HubSpot app) before the module is fully functional.
