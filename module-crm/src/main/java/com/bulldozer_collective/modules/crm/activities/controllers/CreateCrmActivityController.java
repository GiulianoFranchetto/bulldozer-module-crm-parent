package com.bulldozer_collective.modules.crm.activities.controllers;

import com.bulldozer_collective.modules.crm.activities.security.CreateCrmActivityAccessChecker;
import com.bulldozer_collective.modules.crm.activities.services.CreateCrmActivityBuilder;
import com.bulldozer_collective.modules.crm.activities.services.CrmActivityFactory;
import com.bulldozer_collective.modules.crm.api.CreateCrmActivityRequest;
import com.bulldozer_collective.modules.crm.api.CreateCrmActivityResponse;
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
@RequestMapping("/crm/{connectionId}/activities")
public class CreateCrmActivityController {

    private final CrmActivityFactory factory;
    private final CreateCrmActivityAccessChecker accessChecker;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateCrmActivityResponse create(@JwtRequestContext RequestContext context,
                                            @PathVariable @NonNull UUID connectionId,
                                            @Valid @RequestBody @NonNull CreateCrmActivityRequest request) throws NoTokenException {
        this.accessChecker.checkAccess(context);
        var activity = this.factory.create(
                Objects.requireNonNull(context.targetCustomerId()),
                Objects.requireNonNull(context.projectId()),
                connectionId,
                CreateCrmActivityBuilder.builder()
                        .type(request.type())
                        .timestamp(request.timestamp())
                        .title(request.title())
                        .body(request.body())
                        .status(request.status())
                        .ownerId(request.ownerId())
                        .associations(request.associations() == null ? List.of() : request.associations())
                        .build());
        return new CreateCrmActivityResponse(activity);
    }
}
