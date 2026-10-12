package com.bulldozer_collective.modules.crm.containers.controllers;

import com.bulldozer_collective.modules.crm.api.CreateCrmContainersResponse;
import com.bulldozer_collective.modules.crm.api.ImportCrmContainersRequest;
import com.bulldozer_collective.modules.crm.containers.security.ImportCrmContainersAccessChecker;
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
public class ImportCrmContainersController {

    private final CrmContainerFactory factory;
    private final CrmContainerConverter converter;
    private final ImportCrmContainersAccessChecker accessChecker;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateCrmContainersResponse create(@JwtRequestContext RequestContext context,
                                              @PathVariable @NonNull UUID connectionId,
                                              @Valid @RequestBody @NonNull ImportCrmContainersRequest request) throws NoTokenException {
        this.accessChecker.checkAccess(context);
        var containers = this.factory.create(
                Objects.requireNonNull(context.targetCustomerId()),
                Objects.requireNonNull(context.projectId()),
                connectionId,
                request.containers().stream()
                        .map(container -> ImportCrmContainerBuilder.builder()
                                .type(container.type())
                                .remoteId(container.remoteId())
                                .stagePropertyName(container.stagePropertyName())
                                .build())
                        .toList());
        return new CreateCrmContainersResponse(this.converter.convert(containers));
    }
}
