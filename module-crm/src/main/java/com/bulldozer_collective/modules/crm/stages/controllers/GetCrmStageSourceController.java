package com.bulldozer_collective.modules.crm.stages.controllers;

import com.bulldozer_collective.modules.crm.api.GetCrmStageSourceResponse;
import com.bulldozer_collective.modules.crm.stages.security.GetCrmStageSourceAccessChecker;
import com.bulldozer_collective.modules.crm.stages.services.CrmStageSourceConverter;
import com.bulldozer_collective.modules.crm.stages.services.CrmStageSourceFetcher;
import com.bulldozer_collective.security.annotations.JwtRequestContext;
import com.bulldozer_collective.security.resolvers.RequestContext;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/crm/{connectionId}/pipelines/{pipelineId}/stage-source")
public class GetCrmStageSourceController {

    private final CrmStageSourceFetcher fetcher;
    private final CrmStageSourceConverter converter;
    private final GetCrmStageSourceAccessChecker accessChecker;

    @GetMapping
    public GetCrmStageSourceResponse get(@JwtRequestContext RequestContext context,
                                         @PathVariable @NonNull UUID connectionId,
                                         @PathVariable @NonNull String pipelineId) {
        this.accessChecker.checkAccess(context);
        var source = this.fetcher.fetch(
                Objects.requireNonNull(context.targetCustomerId()),
                Objects.requireNonNull(context.projectId()),
                connectionId,
                pipelineId);
        return new GetCrmStageSourceResponse(this.converter.convert(source));
    }
}
