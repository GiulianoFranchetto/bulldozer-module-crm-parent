package com.bulldozer_collective.modules.crm.containers.controllers;

import com.bulldozer_collective.modules.crm.api.CrmContainerParam;
import com.bulldozer_collective.modules.crm.api.ListCrmContainerStagesResponse;
import com.bulldozer_collective.modules.crm.containers.security.ListCrmContainerStagesAccessChecker;
import com.bulldozer_collective.modules.crm.containers.services.CrmContainerFetcher;
import com.bulldozer_collective.modules.crm.containers.services.CrmContainerStageConverter;
import com.bulldozer_collective.security.annotations.JwtRequestContext;
import com.bulldozer_collective.security.resolvers.RequestContext;
import jakarta.validation.Valid;
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
@RequestMapping("/crm/{connectionId}/stages")
public class ListCrmContainerStagesController {

    private final CrmContainerFetcher fetcher;
    private final CrmContainerStageConverter converter;
    private final ListCrmContainerStagesAccessChecker accessChecker;

    @GetMapping
    public ListCrmContainerStagesResponse list(@JwtRequestContext RequestContext context,
                                               @PathVariable @NonNull UUID connectionId,
                                               @Valid @NonNull CrmContainerParam param) {
        this.accessChecker.checkAccess(context);
        var container = this.fetcher.fetch(
                Objects.requireNonNull(context.targetCustomerId()),
                Objects.requireNonNull(context.projectId()),
                connectionId,
                param.containerId());
        return new ListCrmContainerStagesResponse(this.converter.convert(this.fetcher.fetchStages(container)));
    }
}
