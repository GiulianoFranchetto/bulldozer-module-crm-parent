package com.bulldozer_collective.modules.crm.api;

/** Bulldozer's own lead stages, which the stages of every CRM lead pipeline are mapped to. */
public enum CrmLeadStage {
    CRM_LEAD_STAGE_NEW,
    CRM_LEAD_STAGE_ATTEMPTING,
    CRM_LEAD_STAGE_CONNECTED,
    CRM_LEAD_STAGE_QUALIFIED,
    CRM_LEAD_STAGE_DISQUALIFIED
}
