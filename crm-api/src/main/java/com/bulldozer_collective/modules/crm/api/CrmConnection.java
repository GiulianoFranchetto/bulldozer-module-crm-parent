package com.bulldozer_collective.modules.crm.api;

import com.bulldozer_collective.shared.IntegrationPlatform;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import org.jspecify.annotations.NonNull;

@Builder
public record CrmConnection(@NonNull UUID id,
                            @NonNull IntegrationPlatform platform,
                            @NonNull Instant createdAt) {
}
