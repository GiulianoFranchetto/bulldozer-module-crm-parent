package com.bulldozer_collective.modules.crm.api;

import lombok.Builder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * One value of a container's stage source property, as the CRM stores it ({@code remoteId}) and labels it
 * ({@code name}), with the Bulldozer stage it is mapped to: {@code dealStage} in a deal container,
 * {@code leadStage} in a lead container, {@code null} while unmapped. When several values map to the same
 * stage, the one marked {@code writeDefault} is written when a record is set to that stage.
 */
@Builder
public record CrmContainerStage(@NonNull UUID id,
                                @NonNull String remoteId,
                                @NonNull String name,
                                int displayOrder,
                                @Nullable CrmDealStage dealStage,
                                @Nullable CrmLeadStage leadStage,
                                boolean writeDefault) {
}
