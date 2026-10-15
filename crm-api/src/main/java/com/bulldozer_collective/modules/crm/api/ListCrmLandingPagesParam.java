package com.bulldozer_collective.modules.crm.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

/** {@code limit} is nullable and defaulted: an absent parameter must give the first page, not a {@code 400}. */
public record ListCrmLandingPagesParam(@Nullable @Size(max = 4096) String cursor,
                                       @Nullable @Min(1) @Max(CrmConstraints.MAX_LIMIT) Integer limit) {

    public ListCrmLandingPagesParam {
        limit = limit == null ? CrmConstraints.DEFAULT_LIMIT : limit;
    }
}
