package com.bulldozer_collective.modules.crm.api;

/**
 * @param created   records the CRM did not have.
 * @param updated   records the CRM had, given at least one of their empty fields or a new association.
 * @param unchanged records the CRM had, left as they were.
 */
public record CrmImportCounts(int created, int updated, int unchanged) {
}
