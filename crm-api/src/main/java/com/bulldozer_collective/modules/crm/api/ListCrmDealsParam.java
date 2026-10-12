package com.bulldozer_collective.modules.crm.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * Lists the deals of one container, through the CRM's search (indexed with a few seconds' delay). {@code q} is a
 * full-text search; {@code associatedType} + {@code associatedId} restrict to the deals associated with that
 * record. {@code limit} is nullable and defaulted: an absent parameter must give the first page, not a {@code 400}.
 */
public record ListCrmDealsParam(@NonNull @NotNull UUID containerId,
        @Nullable @Size(max = 3000) String q,
        @Nullable CrmObjectType associatedType,
        @Nullable @Size(max = CrmConstraints.SHORT_TEXT) String associatedId,
        @Nullable @Size(max = 4096) String cursor,
        @Nullable @Min(1) @Max(CrmConstraints.MAX_LIMIT) Integer limit) {

    public ListCrmDealsParam {
        limit = limit == null ? CrmConstraints.DEFAULT_LIMIT : limit;
    }
}
