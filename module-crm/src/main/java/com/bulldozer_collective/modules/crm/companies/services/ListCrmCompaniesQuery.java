package com.bulldozer_collective.modules.crm.companies.services;

import com.bulldozer_collective.shared.IntegrationPlatform;
import lombok.Builder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

@Builder
public record ListCrmCompaniesQuery(@NonNull UUID customerId,
                                    @NonNull UUID projectId,
                                    @Nullable IntegrationPlatform platform,
                                    @Nullable String q,
                                    int page,
                                    int size) {
}
