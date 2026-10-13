package com.bulldozer_collective.modules.crm.api;

import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * What an import did in the CRM.
 *
 * @param skipped the leads not imported, or only partly, and why.
 */
public record ImportCrmLeadListResponse(@NonNull CrmImportCounts contacts,
                                        @NonNull CrmImportCounts companies,
                                        @NonNull List<CrmImportSkip> skipped) {
}
