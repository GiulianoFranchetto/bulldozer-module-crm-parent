package com.bulldozer_collective.modules.crm.containers.controllers;

import com.bulldozer_collective.modules.crm.api.CrmContainerParam;
import com.bulldozer_collective.modules.crm.api.GetCrmContainerStageResponse;
import com.bulldozer_collective.modules.crm.api.MapCrmContainerStageRequest;
import com.bulldozer_collective.modules.crm.containers.security.MapCrmContainerStageAccessChecker;
import com.bulldozer_collective.modules.crm.containers.services.CrmContainerStageConverter;
import com.bulldozer_collective.modules.crm.containers.services.CrmContainerStageUpdater;
import com.bulldozer_collective.modules.crm.containers.services.MapCrmContainerStageBuilder;
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
@RequestMapping("/crm/{connectionId}/stages")
public class MapCrmContainerStageController {

    private final CrmContainerStageUpdater updater;
    private final CrmContainerStageConverter converter;
    private final MapCrmContainerStageAccessChecker accessChecker;

    @PutMapping("/{stageId}/mapping")
    public GetCrmContainerStageResponse map(@JwtRequestContext RequestContext context,
                                            @PathVariable @NonNull UUID connectionId,
                                            @PathVariable @NonNull UUID stageId,
                                            @Valid @NonNull CrmContainerParam param,
                                            @Valid @RequestBody @NonNull MapCrmContainerStageRequest request) {
        this.accessChecker.checkAccess(context);
        var stage = this.updater.map(
                Objects.requireNonNull(context.targetCustomerId()),
                Objects.requireNonNull(context.projectId()),
                connectionId,
                param.containerId(),
                stageId,
                MapCrmContainerStageBuilder.builder()
                        .dealStage(request.dealStage())
                        .leadStage(request.leadStage())
                        .writeDefault(request.writeDefault())
                        .build());
        return new GetCrmContainerStageResponse(this.converter.convert(stage));
    }
}
