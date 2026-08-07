package com.bulldozer_collective.modules.crm.api;

import com.bulldozer_collective.shared.IntegrationPlatform;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.jspecify.annotations.Nullable;

/**
 * {@code page} and {@code size} are nullable and defaulted by the canonical constructor: a caller that sends
 * neither must get the first page, not a {@code 400}. They were primitives, which Spring bound to {@code 0}
 * for an absent parameter, and {@code PageRequest.of(0, 0)} throws.
 */
public record ListCrmCompaniesParam(
        @Nullable String q,
        @Nullable IntegrationPlatform platform,
        @Nullable @Min(0) Integer page,
        @Nullable @Min(1) @Max(CrmConstraints.MAX_PAGE_SIZE) Integer size
) {
    public ListCrmCompaniesParam {
        page = page == null ? 0 : page;
        size = size == null ? CrmConstraints.DEFAULT_PAGE_SIZE : size;
    }
}
