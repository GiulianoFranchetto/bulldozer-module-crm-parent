package com.bulldozer_collective.modules.crm.api;

import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.NonNull;

public record GetCrmActivityParam(@NonNull @NotNull CrmActivityType type) {
}
