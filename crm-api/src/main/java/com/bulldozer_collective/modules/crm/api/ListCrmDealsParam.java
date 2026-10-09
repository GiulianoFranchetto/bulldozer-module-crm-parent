package com.bulldozer_collective.modules.crm.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

/**
 * {@code q} is a full-text search on the CRM's searchable properties; {@code associatedType} + {@code associatedId}
 * restrict the listing to the records associated with that one. Both go through the CRM's search, which indexes
 * a write with a few seconds' delay. {@code limit} is nullable and defaulted: an absent parameter must give the
 * first page, not a {@code 400}.
 */
public record ListCrmDealsParam(@Nullable @Size(max = 3000) String q,
        @Nullable CrmObjectType associatedType,
        @Nullable @Size(max = CrmConstraints.SHORT_TEXT) String associatedId,
        @Nullable @Size(max = 4096) String cursor,
        @Nullable @Min(1) @Max(CrmConstraints.MAX_LIMIT) Integer limit) {

    public ListCrmDealsParam {
        limit = limit == null ? CrmConstraints.DEFAULT_LIMIT : limit;
    }
}
