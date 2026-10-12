package com.bulldozer_collective.modules.crm.api;

import org.jspecify.annotations.Nullable;

/** {@code type} restricts the listing to deal or lead containers. */
public record ListCrmContainersParam(@Nullable CrmContainerType type) {
}
