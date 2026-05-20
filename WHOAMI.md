# Goal of this module
The goal of this module is to create a server holding CRM data from Bulldozer customer.
The 3 targeted CRM are:
- SalesForce
- Hubspot
- PipeDrive

We need to implement a common data model that will be shared accross all CRM. For example, we shall not store `HubspotDeal`, `SalesforceDeal` and `PipedriveDeal`, but `CrmDeal` instead.

We need to be able to store:
- contacts
- deals
- companies

We need to create a correspondance table between our newly defined data model, and the one exposed by the different CRM.

One example is the deal stage. We need to defined our own deal stage, like `PENDING`, `WON`, `LOST`... and the one used by the targeted CRM. This correspondance table must be a user input.