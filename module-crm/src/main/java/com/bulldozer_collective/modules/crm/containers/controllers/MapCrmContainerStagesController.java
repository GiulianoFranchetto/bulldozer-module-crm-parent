package com.bulldozer_collective.modules.crm.containers.controllers;

import com.bulldozer_collective.modules.crm.api.CrmContainerParam;
import com.bulldozer_collective.modules.crm.api.ListCrmContainerStagesResponse;
import com.bulldozer_collective.modules.crm.api.MapCrmContainerStagesRequest;
import com.bulldozer_collective.modules.crm.containers.security.MapCrmContainerStagesAccessChecker;
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
public class MapCrmContainerStagesController {

    private final CrmContainerStageUpdater updater;
    private final CrmContainerStageConverter converter;
    private final MapCrmContainerStagesAccessChecker accessChecker;

    @PutMapping("/mapping")
    public ListCrmContainerStagesResponse map(@JwtRequestContext RequestContext context,
                                              @PathVariable @NonNull UUID connectionId,
                                              @Valid @NonNull CrmContainerParam param,
                                              @Valid @RequestBody @NonNull MapCrmContainerStagesRequest request) {
        this.accessChecker.checkAccess(context);
        var stages = this.updater.map(
                Objects.requireNonNull(context.targetCustomerId()),
                Objects.requireNonNull(context.projectId()),
                connectionId,
                param.containerId(),
                request.mappings().stream()
                        .map(mapping -> MapCrmContainerStageBuilder.builder()
                                .stageId(mapping.stageId())
                                .dealStage(mapping.dealStage())
                                .leadStage(mapping.leadStage())
                                .writeDefault(mapping.writeDefault())
                                .build())
                        .toList());
        return new ListCrmContainerStagesResponse(this.converter.convert(stages));
    }
}
