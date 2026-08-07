package com.bulldozer_collective.modules.crm.pipelines.controllers;

import com.bulldozer_collective.modules.crm.api.DiscoverCrmPipelinesRequest;
import com.bulldozer_collective.modules.crm.api.ListCrmPipelinesResponse;
import com.bulldozer_collective.modules.crm.pipelines.security.DiscoverCrmPipelinesAccessChecker;
import com.bulldozer_collective.modules.crm.pipelines.services.CrmPipelineConverter;
import com.bulldozer_collective.modules.crm.pipelines.services.CrmPipelineDiscoveryService;
import com.bulldozer_collective.modules.crm.sync.services.NoTokenException;
import com.bulldozer_collective.security.annotations.JwtRequestContext;
import com.bulldozer_collective.security.resolvers.RequestContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequiredArgsConstructor
@RequestMapping("/crm/pipelines")
public class DiscoverCrmPipelinesController {

    private final CrmPipelineDiscoveryService discoveryService;
    private final CrmPipelineConverter converter;
    private final DiscoverCrmPipelinesAccessChecker accessChecker;

    @PostMapping("/discover")
    public ListCrmPipelinesResponse discover(@JwtRequestContext RequestContext context,
                                             @Valid @RequestBody DiscoverCrmPipelinesRequest request) throws NoTokenException {
        this.accessChecker.checkAccess(context);
        var pipelines = this.discoveryService.discoverAndFetch(
                Objects.requireNonNull(context.targetCustomerId()),
                Objects.requireNonNull(context.projectId()),
                request.platform()
        );
        return new ListCrmPipelinesResponse(this.converter.convert(pipelines));
    }
}
