package com.bulldozer_collective.modules.crm.pipelines.controllers;

import com.bulldozer_collective.modules.crm.api.CreateCrmStageMappingRequest;
import com.bulldozer_collective.modules.crm.pipelines.security.CreateCrmStageMappingAccessChecker;
import com.bulldozer_collective.modules.crm.pipelines.services.CreateCrmStageMappingBuilder;
import com.bulldozer_collective.modules.crm.pipelines.services.CrmStageMappingFactory;
import com.bulldozer_collective.security.annotations.JwtRequestContext;
import com.bulldozer_collective.security.resolvers.RequestContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequiredArgsConstructor
@RequestMapping("/crm/stage-mappings")
public class CreateCrmStageMappingController {

    private final CrmStageMappingFactory factory;
    private final CreateCrmStageMappingAccessChecker accessChecker;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void create(@JwtRequestContext RequestContext context,
                       @Valid @RequestBody CreateCrmStageMappingRequest request) {
        this.accessChecker.checkAccess(context);
        this.factory.create(CreateCrmStageMappingBuilder.builder()
                .customerId(Objects.requireNonNull(context.targetCustomerId()))
                .projectId(Objects.requireNonNull(context.projectId()))
                .pipelineStageId(request.pipelineStageId())
                .internalStage(request.internalStage())
                .build());
    }
}
