package com.bulldozer_collective.modules.crm.api;

import org.jspecify.annotations.NonNull;

import java.util.List;

public record ListCrmPipelinesResponse(
        @NonNull List<CrmPipeline> pipelines
) {}
