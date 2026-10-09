# Goal of this module
This module is a bridge between Bulldozer and the CRM of a Bulldozer customer. It reads and writes the
customer's CRM in real time and stores none of its data: only the configuration needed to reach the CRM and to
translate its deal stages.

The targeted CRMs are:
- HubSpot (implemented)
- Salesforce (to come)

We expose a common data model shared across all CRMs. For example, we never expose `HubspotDeal` or
`SalesforceDeal`, but `CrmDeal` instead, behind one façade per CRM (`CrmGateway`).

Objects bridged:
- contacts
- companies
- deals and pipelines
- activities (calls, emails, meetings, notes, tasks)
- associations between them

The deal stage is the one piece of CRM semantics we translate. Bulldozer defines its own stages
(`CRM_DEAL_STAGE_WON`, `CRM_DEAL_STAGE_LOST`…). For each pipeline, the customer chooses which deal property
holds the stage — the CRM's native one or a custom property — and maps its values to ours. That correspondence
table is a user input; we never assume the CRM's default stages are the ones in use.
