package com.bulldozer_collective.modules.crm.containers.controllers;

import com.bulldozer_collective.modules.crm.api.GetCrmContainerResponse;
import com.bulldozer_collective.modules.crm.api.SetCrmStageSourceRequest;
import com.bulldozer_collective.modules.crm.containers.security.SetCrmContainerStageSourceAccessChecker;
import com.bulldozer_collective.modules.crm.containers.services.CrmContainerConverter;
import com.bulldozer_collective.modules.crm.containers.services.CrmContainerUpdater;
import com.bulldozer_collective.modules.crm.gateway.NoTokenException;
import com.bulldozer_collective.security.annotations.JwtRequestContext;
import com.bulldozer_collective.security.resolvers.RequestContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/crm/{connectionId}/containers")
public class SetCrmContainerStageSourceController {

    private final CrmContainerUpdater updater;
    private final CrmContainerConverter converter;
    private final SetCrmContainerStageSourceAccessChecker accessChecker;

    @PutMapping("/{containerId}/stage-source")
    public GetCrmContainerResponse set(@JwtRequestContext RequestContext context,
                                       @PathVariable @NonNull UUID connectionId,
                                       @PathVariable @NonNull UUID containerId,
                                       @Valid @RequestBody @NonNull SetCrmStageSourceRequest request) throws NoTokenException {
        this.accessChecker.checkAccess(context);
        var container = this.updater.setStageSource(
                Objects.requireNonNull(context.targetCustomerId()),
                Objects.requireNonNull(context.projectId()),
                connectionId,
                containerId,
                request.propertyName());
        return new GetCrmContainerResponse(this.converter.convert(container));
    }
}
