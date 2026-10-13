package com.bulldozer_collective.modules.crm.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * @param leadListId a lead list of the project (outbound module); only its accepted leads are imported.
 * @param legalBasis written on every contact created, as {@code legalBasis} on contact creation — required by a CRM
 *                   portal with its GDPR features on.
 */
public record ImportCrmLeadListRequest(@NonNull @NotNull UUID leadListId,
                                       @Nullable @Size(max = CrmConstraints.SHORT_TEXT) String legalBasis) {
}
