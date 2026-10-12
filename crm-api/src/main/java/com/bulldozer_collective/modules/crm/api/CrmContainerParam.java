package com.bulldozer_collective.modules.crm.api;

import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.UUID;

/** The container a contained object (deal, lead, stage) is read or written through. */
public record CrmContainerParam(@NonNull @NotNull UUID containerId) {
}
