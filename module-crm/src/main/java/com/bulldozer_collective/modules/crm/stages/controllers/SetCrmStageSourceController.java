package com.bulldozer_collective.modules.crm.stages.controllers;

import com.bulldozer_collective.modules.crm.api.GetCrmStageSourceResponse;
import com.bulldozer_collective.modules.crm.api.SetCrmStageSourceRequest;
import com.bulldozer_collective.modules.crm.gateway.NoTokenException;
import com.bulldozer_collective.modules.crm.stages.security.SetCrmStageSourceAccessChecker;
import com.bulldozer_collective.modules.crm.stages.services.CrmStageSourceConverter;
import com.bulldozer_collective.modules.crm.stages.services.CrmStageSourceUpdater;
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
@RequestMapping("/crm/{connectionId}/pipelines/{pipelineId}/stage-source")
public class SetCrmStageSourceController {

    private final CrmStageSourceUpdater updater;
    private final CrmStageSourceConverter converter;
    private final SetCrmStageSourceAccessChecker accessChecker;

    @PutMapping
    public GetCrmStageSourceResponse set(@JwtRequestContext RequestContext context,
                                         @PathVariable @NonNull UUID connectionId,
                                         @PathVariable @NonNull String pipelineId,
                                         @Valid @RequestBody @NonNull SetCrmStageSourceRequest request) throws NoTokenException {
        this.accessChecker.checkAccess(context);
        var source = this.updater.set(
                Objects.requireNonNull(context.targetCustomerId()),
                Objects.requireNonNull(context.projectId()),
                connectionId,
                pipelineId,
                request.propertyName());
        return new GetCrmStageSourceResponse(this.converter.convert(source));
    }
}
