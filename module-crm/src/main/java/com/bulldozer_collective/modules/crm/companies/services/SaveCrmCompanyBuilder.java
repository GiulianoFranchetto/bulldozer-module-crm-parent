package com.bulldozer_collective.modules.crm.companies.services;

import lombok.Builder;
import org.jspecify.annotations.Nullable;

import java.util.stream.Stream;

/** The fields of a company to write to the CRM. A {@code null} field is not written. */
@Builder
public record SaveCrmCompanyBuilder(@Nullable String name,
                                    @Nullable String domain,
                                    @Nullable String industry,
                                    @Nullable String phone,
                                    @Nullable String city,
                                    @Nullable String country,
                                    @Nullable String ownerId) {

    public boolean isEmpty() {
        return Stream.of(this.name, this.domain, this.industry, this.phone, this.city, this.country, this.ownerId)
                .allMatch(value -> value == null);
    }
}
