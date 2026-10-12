package com.bulldozer_collective.modules.crm.containers.controllers;

import com.bulldozer_collective.modules.crm.api.GetCrmContainerResponse;
import com.bulldozer_collective.modules.crm.containers.security.RefreshCrmContainerAccessChecker;
import com.bulldozer_collective.modules.crm.containers.services.CrmContainerConverter;
import com.bulldozer_collective.modules.crm.containers.services.CrmContainerUpdater;
import com.bulldozer_collective.modules.crm.gateway.NoTokenException;
import com.bulldozer_collective.security.annotations.JwtRequestContext;
import com.bulldozer_collective.security.resolvers.RequestContext;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/crm/{connectionId}/containers")
public class RefreshCrmContainerController {

    private final CrmContainerUpdater updater;
    private final CrmContainerConverter converter;
    private final RefreshCrmContainerAccessChecker accessChecker;

    @PostMapping("/{containerId}/refresh")
    public GetCrmContainerResponse refresh(@JwtRequestContext RequestContext context,
                                           @PathVariable @NonNull UUID connectionId,
                                           @PathVariable @NonNull UUID containerId) throws NoTokenException {
        this.accessChecker.checkAccess(context);
        var container = this.updater.refresh(
                Objects.requireNonNull(context.targetCustomerId()),
                Objects.requireNonNull(context.projectId()),
                connectionId,
                containerId);
        return new GetCrmContainerResponse(this.converter.convert(container));
    }
}
