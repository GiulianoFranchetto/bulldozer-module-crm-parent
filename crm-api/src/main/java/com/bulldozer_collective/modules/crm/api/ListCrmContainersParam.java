package com.bulldozer_collective.modules.crm.api;

import org.jspecify.annotations.Nullable;

/** {@code type} restricts the listing to the containers of deals, or of leads; absent, both are listed. */
public record ListCrmContainersParam(@Nullable CrmContainerType type) {
}
