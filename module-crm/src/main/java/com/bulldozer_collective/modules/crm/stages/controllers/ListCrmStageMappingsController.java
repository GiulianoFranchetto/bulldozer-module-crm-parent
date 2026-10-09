package com.bulldozer_collective.modules.crm.stages.controllers;

import com.bulldozer_collective.modules.crm.api.ListCrmStageMappingsResponse;
import com.bulldozer_collective.modules.crm.stages.security.ListCrmStageMappingsAccessChecker;
import com.bulldozer_collective.modules.crm.stages.services.CrmStageMappingConverter;
import com.bulldozer_collective.modules.crm.stages.services.CrmStageMappingFetcher;
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
@RequestMapping("/crm/{connectionId}/pipelines/{pipelineId}/stage-mappings")
public class ListCrmStageMappingsController {

    private final CrmStageMappingFetcher fetcher;
    private final CrmStageMappingConverter converter;
    private final ListCrmStageMappingsAccessChecker accessChecker;

    @GetMapping
    public ListCrmStageMappingsResponse list(@JwtRequestContext RequestContext context,
                                             @PathVariable @NonNull UUID connectionId,
                                             @PathVariable @NonNull String pipelineId) {
        this.accessChecker.checkAccess(context);
        var mappings = this.fetcher.fetchAll(
                Objects.requireNonNull(context.targetCustomerId()),
                Objects.requireNonNull(context.projectId()),
                connectionId,
                pipelineId);
        return new ListCrmStageMappingsResponse(this.converter.convert(mappings));
    }
}
