package com.bulldozer_collective.modules.crm.containers.controllers;

import com.bulldozer_collective.modules.crm.api.CreateCrmPropertyRequest;
import com.bulldozer_collective.modules.crm.api.CreateCrmPropertyResponse;
import com.bulldozer_collective.modules.crm.containers.security.CreateCrmPropertyAccessChecker;
import com.bulldozer_collective.modules.crm.containers.services.CreateCrmPropertyBuilder;
import com.bulldozer_collective.modules.crm.containers.services.CrmPropertyFactory;
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

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/crm/{connectionId}/properties")
public class CreateCrmPropertyController {

    private final CrmPropertyFactory factory;
    private final CreateCrmPropertyAccessChecker accessChecker;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateCrmPropertyResponse create(@JwtRequestContext RequestContext context,
                                            @PathVariable @NonNull UUID connectionId,
                                            @Valid @RequestBody @NonNull CreateCrmPropertyRequest request) throws NoTokenException {
        this.accessChecker.checkAccess(context);
        var options = request.options() == null ? List.<CreateCrmPropertyBuilder.Option>of() : request.options().stream()
                .map(option -> new CreateCrmPropertyBuilder.Option(option.label(), option.value()))
                .toList();
        var property = this.factory.create(
                Objects.requireNonNull(context.targetCustomerId()),
                Objects.requireNonNull(context.projectId()),
                connectionId,
                CreateCrmPropertyBuilder.builder()
                        .objectType(request.objectType())
                        .name(request.name())
                        .label(request.label())
                        .groupName(request.groupName())
                        .description(request.description())
                        .fieldType(request.fieldType())
                        .options(options)
                        .build());
        return new CreateCrmPropertyResponse(property);
    }
}
