# Project definition
This project is a part of the Bulldozer SaaS eco-system.
It must respect all Bulldozer coding style, defined in the global BULLDOZER_BACKEND.md

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
