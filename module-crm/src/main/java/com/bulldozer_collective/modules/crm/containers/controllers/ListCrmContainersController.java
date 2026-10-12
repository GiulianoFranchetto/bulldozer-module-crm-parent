package com.bulldozer_collective.modules.crm.containers.controllers;

import com.bulldozer_collective.modules.crm.api.ListCrmContainersParam;
import com.bulldozer_collective.modules.crm.api.ListCrmContainersResponse;
import com.bulldozer_collective.modules.crm.containers.security.ListCrmContainersAccessChecker;
import com.bulldozer_collective.modules.crm.containers.services.CrmContainerConverter;
import com.bulldozer_collective.modules.crm.containers.services.CrmContainerFetcher;
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
@RequestMapping("/crm/{connectionId}/containers")
public class ListCrmContainersController {

    private final CrmContainerFetcher fetcher;
    private final CrmContainerConverter converter;
    private final ListCrmContainersAccessChecker accessChecker;

    @GetMapping
    public ListCrmContainersResponse list(@JwtRequestContext RequestContext context,
                                          @PathVariable @NonNull UUID connectionId,
                                          @Valid @NonNull ListCrmContainersParam param) {
        this.accessChecker.checkAccess(context);
        var containers = this.fetcher.fetchAll(
                Objects.requireNonNull(context.targetCustomerId()),
                Objects.requireNonNull(context.projectId()),
                connectionId,
                param.type());
        return new ListCrmContainersResponse(this.converter.convert(containers));
    }
}
