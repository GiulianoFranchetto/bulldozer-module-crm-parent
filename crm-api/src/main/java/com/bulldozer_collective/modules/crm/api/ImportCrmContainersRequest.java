package com.bulldozer_collective.modules.crm.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.NonNull;

import java.util.List;

/** Imports several CRM containers at once, all or none. */
public record ImportCrmContainersRequest(
        @NonNull @NotNull @Size(min = 1, max = 50) List<@NotNull @Valid ImportCrmContainerRequest> containers) {
}
