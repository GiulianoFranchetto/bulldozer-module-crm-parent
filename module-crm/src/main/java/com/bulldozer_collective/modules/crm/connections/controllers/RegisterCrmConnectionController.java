package com.bulldozer_collective.modules.crm.connections.controllers;

import com.bulldozer_collective.modules.crm.api.CreateCrmConnectionResponse;
import com.bulldozer_collective.modules.crm.api.RegisterCrmConnectionRequest;
import com.bulldozer_collective.modules.crm.connections.security.RegisterCrmConnectionAccessChecker;
import com.bulldozer_collective.modules.crm.connections.services.CrmConnectionConverter;
import com.bulldozer_collective.modules.crm.connections.services.CrmConnectionFactory;
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
@RequestMapping("/crm/connections")
public class RegisterCrmConnectionController {

    private final CrmConnectionFactory factory;
    private final CrmConnectionConverter converter;
    private final RegisterCrmConnectionAccessChecker accessChecker;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateCrmConnectionResponse register(@JwtRequestContext RequestContext context,
                                                @PathVariable @NonNull UUID connectionId,
                                                @Valid @RequestBody @NonNull RegisterCrmConnectionRequest request) throws NoTokenException {
        this.accessChecker.checkAccess(context);
        var connection = this.factory.create(
                Objects.requireNonNull(context.targetCustomerId()),
                Objects.requireNonNull(context.projectId()),
                request.platform());
        return new CreateCrmConnectionResponse(this.converter.convert(connection));
    }
}
