package com.bulldozer_collective.modules.crm.pipelines.controllers;

import com.bulldozer_collective.modules.crm.api.ListCrmPipelinesParam;
import com.bulldozer_collective.modules.crm.api.ListCrmPipelinesResponse;
import com.bulldozer_collective.modules.crm.pipelines.security.ListCrmPipelinesAccessChecker;
import com.bulldozer_collective.modules.crm.pipelines.services.CrmPipelineConverter;
import com.bulldozer_collective.modules.crm.pipelines.services.CrmPipelineFetcher;
import com.bulldozer_collective.security.annotations.JwtRequestContext;
import jakarta.validation.Valid;
import com.bulldozer_collective.security.resolvers.RequestContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequiredArgsConstructor
@RequestMapping("/crm/pipelines")
public class ListCrmPipelinesController {

    private final CrmPipelineFetcher fetcher;
    private final CrmPipelineConverter converter;
    private final ListCrmPipelinesAccessChecker accessChecker;

    @GetMapping
    public ListCrmPipelinesResponse list(@JwtRequestContext RequestContext context,
                                         @Valid ListCrmPipelinesParam param) {
        this.accessChecker.checkAccess(context);
        var pipelines = this.fetcher.fetchAll(
                Objects.requireNonNull(context.targetCustomerId()),
                Objects.requireNonNull(context.projectId()),
                param.platform()
        );
        return new ListCrmPipelinesResponse(this.converter.convert(pipelines));
    }
}
