package com.bulldozer_collective.modules.crm.containers.controllers;

import com.bulldozer_collective.modules.crm.api.CreateCrmContainerResponse;
import com.bulldozer_collective.modules.crm.api.ImportCrmContainerRequest;
import com.bulldozer_collective.modules.crm.containers.security.ImportCrmContainerAccessChecker;
import com.bulldozer_collective.modules.crm.containers.services.CrmContainerConverter;
import com.bulldozer_collective.modules.crm.containers.services.CrmContainerFactory;
import com.bulldozer_collective.modules.crm.containers.services.ImportCrmContainerBuilder;
import com.bulldozer_collective.modules.crm.gateway.NoTokenException;
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
@RequestMapping("/crm/{connectionId}/containers")
public class ImportCrmContainerController {

    private final CrmContainerFactory factory;
    private final CrmContainerConverter converter;
    private final ImportCrmContainerAccessChecker accessChecker;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateCrmContainerResponse create(@JwtRequestContext RequestContext context,
                                             @PathVariable @NonNull UUID connectionId,
                                             @Valid @RequestBody @NonNull ImportCrmContainerRequest request) throws NoTokenException {
        this.accessChecker.checkAccess(context);
        var container = this.factory.create(
                Objects.requireNonNull(context.targetCustomerId()),
                Objects.requireNonNull(context.projectId()),
                connectionId,
                ImportCrmContainerBuilder.builder()
                        .type(request.type())
                        .remoteId(request.remoteId())
                        .stagePropertyName(request.stagePropertyName())
                        .build());
        return new CreateCrmContainerResponse(this.converter.convert(container));
    }
}
