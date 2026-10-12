package com.bulldozer_collective.modules.crm.api;

import lombok.Builder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

/**
 * A lead as read from the CRM. {@code id} is the CRM's own id; {@code containerId} is the Bulldozer container
 * (the CRM lead pipeline) the lead was read through. {@code type} is the CRM's raw lead type.
 *
 * <p>{@code stageValue} is the raw value of the container's stage source property; {@code stage} is the stage
 * that value is mapped to, {@code null} while it is not mapped.
 */
@Builder(toBuilder = true)
public record CrmLead(@NonNull String id,
                      @Nullable UUID containerId,
                      @Nullable String name,
                      @Nullable String type,
                      @Nullable CrmLeadStage stage,
                      @Nullable String stageValue,
                      @Nullable Instant createdAt,
                      @Nullable Instant updatedAt) {
}
