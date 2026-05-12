package com.bulldozer_collective.modules.crm.api;

import org.jspecify.annotations.NonNull;

import java.util.UUID;

public record CrmSyncRunStartedResponse(
        @NonNull UUID syncRunId
) {}
