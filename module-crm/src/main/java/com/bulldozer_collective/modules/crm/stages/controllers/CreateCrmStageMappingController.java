package com.bulldozer_collective.modules.crm.stages.controllers;

import com.bulldozer_collective.modules.crm.api.CreateCrmStageMappingRequest;
import com.bulldozer_collective.modules.crm.api.CreateCrmStageMappingResponse;
import com.bulldozer_collective.modules.crm.gateway.NoTokenException;
import com.bulldozer_collective.modules.crm.stages.security.CreateCrmStageMappingAccessChecker;
import com.bulldozer_collective.modules.crm.stages.services.CreateCrmStageMappingBuilder;
import com.bulldozer_collective.modules.crm.stages.services.CrmStageMappingConverter;
import com.bulldozer_collective.modules.crm.stages.services.CrmStageMappingFactory;
import com.bulldozer_collective.security.annotations.JwtRequestContext;
import com.bulldozer_collective.security.resolvers.RequestContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/crm/{connectionId}/pipelines/{pipelineId}/stage-mappings")
public class CreateCrmStageMappingController {

    private final CrmStageMappingFactory factory;
    private final CrmStageMappingConverter converter;
    private final CreateCrmStageMappingAccessChecker accessChecker;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateCrmStageMappingResponse create(@JwtRequestContext RequestContext context,
                                                @PathVariable @NonNull UUID connectionId,
                                                @PathVariable @NonNull String pipelineId,
                                                @Valid @RequestBody @NonNull CreateCrmStageMappingRequest request) throws NoTokenException {
        this.accessChecker.checkAccess(context);
        var mapping = this.factory.create(
                Objects.requireNonNull(context.targetCustomerId()),
                Objects.requireNonNull(context.projectId()),
                connectionId,
                pipelineId,
                CreateCrmStageMappingBuilder.builder()
                        .value(request.value())
                        .stage(request.stage())
                        .writeDefault(request.writeDefault())
                        .build());
        return new CreateCrmStageMappingResponse(this.converter.convert(mapping));
    }
}
