package com.bulldozer_collective.modules.crm.containers.controllers;

import com.bulldozer_collective.modules.crm.api.GetCrmContainerResponse;
import com.bulldozer_collective.modules.crm.containers.security.GetCrmContainerAccessChecker;
import com.bulldozer_collective.modules.crm.containers.services.CrmContainerConverter;
import com.bulldozer_collective.modules.crm.containers.services.CrmContainerFetcher;
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
@RequestMapping("/crm/{connectionId}/containers")
public class GetCrmContainerController {

    private final CrmContainerFetcher fetcher;
    private final CrmContainerConverter converter;
    private final GetCrmContainerAccessChecker accessChecker;

    @GetMapping("/{containerId}")
    public GetCrmContainerResponse get(@JwtRequestContext RequestContext context,
                                       @PathVariable @NonNull UUID connectionId,
                                       @PathVariable @NonNull UUID containerId) {
        this.accessChecker.checkAccess(context);
        var container = this.fetcher.fetch(
                Objects.requireNonNull(context.targetCustomerId()),
                Objects.requireNonNull(context.projectId()),
                connectionId,
                containerId);
        return new GetCrmContainerResponse(this.converter.convert(container));
    }
}
