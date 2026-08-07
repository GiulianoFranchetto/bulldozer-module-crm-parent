package com.bulldozer_collective.modules.crm.contacts.services;

import com.bulldozer_collective.shared.IntegrationPlatform;
import lombok.Builder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

@Builder
public record ListCrmContactsQuery(@NonNull UUID customerId,
                                   @NonNull UUID projectId,
                                   @Nullable IntegrationPlatform platform,
                                   @Nullable String q,
                                   int page,
                                   int size) {
}
