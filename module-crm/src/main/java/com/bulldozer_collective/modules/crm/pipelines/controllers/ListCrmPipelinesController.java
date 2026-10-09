package com.bulldozer_collective.modules.crm.pipelines.controllers;

import com.bulldozer_collective.modules.crm.api.ListCrmPipelinesResponse;
import com.bulldozer_collective.modules.crm.gateway.NoTokenException;
import com.bulldozer_collective.modules.crm.pipelines.security.ListCrmPipelinesAccessChecker;
import com.bulldozer_collective.modules.crm.pipelines.services.CrmPipelineSearcher;
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
@RequestMapping("/crm/{connectionId}/pipelines")
public class ListCrmPipelinesController {

    private final CrmPipelineSearcher searcher;
    private final ListCrmPipelinesAccessChecker accessChecker;

    @GetMapping
    public ListCrmPipelinesResponse list(@JwtRequestContext RequestContext context,
                                         @PathVariable @NonNull UUID connectionId) throws NoTokenException {
        this.accessChecker.checkAccess(context);
        return new ListCrmPipelinesResponse(this.searcher.listPipelines(
                Objects.requireNonNull(context.targetCustomerId()),
                Objects.requireNonNull(context.projectId()),
                connectionId));
    }
}
