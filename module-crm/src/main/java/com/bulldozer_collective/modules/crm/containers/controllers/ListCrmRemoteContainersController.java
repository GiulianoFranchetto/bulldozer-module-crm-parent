package com.bulldozer_collective.modules.crm.containers.controllers;

import com.bulldozer_collective.modules.crm.api.CrmContainerTypeParam;
import com.bulldozer_collective.modules.crm.api.ListCrmRemoteContainersResponse;
import com.bulldozer_collective.modules.crm.containers.security.ListCrmRemoteContainersAccessChecker;
import com.bulldozer_collective.modules.crm.containers.services.CrmRemoteContainerSearcher;
import com.bulldozer_collective.modules.crm.gateway.NoTokenException;
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
@RequestMapping("/crm/{connectionId}/remote-containers")
public class ListCrmRemoteContainersController {

    private final CrmRemoteContainerSearcher searcher;
    private final ListCrmRemoteContainersAccessChecker accessChecker;

    @GetMapping
    public ListCrmRemoteContainersResponse list(@JwtRequestContext RequestContext context,
                                                @PathVariable @NonNull UUID connectionId,
                                                @Valid @NonNull CrmContainerTypeParam param) throws NoTokenException {
        this.accessChecker.checkAccess(context);
        return new ListCrmRemoteContainersResponse(this.searcher.listContainers(
                Objects.requireNonNull(context.targetCustomerId()),
                Objects.requireNonNull(context.projectId()),
                connectionId,
                param.type()));
    }
}
