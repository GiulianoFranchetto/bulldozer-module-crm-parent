# Project definition
This project is a part of the Bulldozer SaaS eco-system.
It must respect all Bulldozer coding style, defined in the global BULLDOZER_BACKEND.md

# Access control
`security/DefaultAccessChecker` grants access to `BULLDOZER_ADMINISTRATOR`, `BULLDOZER_OPS`, the target
customer's administrator, then any project membership — aligned on the rest of the fleet since Bug 67. The 15
per-endpoint checkers are pure `super.checkAccess(context)` pass-throughs, and no endpoint carries
`@FeatureNeeded`. Full table in `RBAC.md`, to keep in sync whenever an endpoint or a checker changes.