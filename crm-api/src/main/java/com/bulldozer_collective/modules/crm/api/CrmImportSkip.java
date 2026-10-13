package com.bulldozer_collective.modules.crm.api;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/** @param leadId the lead (outbound module) concerned; absent when the CRM did not say which. */
public record CrmImportSkip(@Nullable UUID leadId, @NonNull String reason) {
}
