package com.bulldozer_collective.modules.crm.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.NonNull;

import java.util.List;

/** Replaces the mapping of every listed stage of one container, all or none; unlisted stages are left as they are. */
public record MapCrmContainerStagesRequest(
        @NonNull @NotNull @Size(min = 1, max = 500) List<@NotNull @Valid MapCrmContainerStageRequest> mappings) {
}
