package com.bulldozer_collective.modules.crm.connections.controllers;

import com.bulldozer_collective.modules.crm.api.RegisterCrmConnectionRequest;
import com.bulldozer_collective.modules.crm.connections.security.RegisterCrmConnectionAccessChecker;
import com.bulldozer_collective.modules.crm.connections.services.CrmConnectionFactory;
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
@RequestMapping("/crm/connections")
public class RegisterCrmConnectionController {

    private final CrmConnectionFactory factory;
    private final RegisterCrmConnectionAccessChecker accessChecker;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void register(@JwtRequestContext RequestContext context,
                         @Valid @RequestBody RegisterCrmConnectionRequest request) {
        this.accessChecker.checkAccess(context);
        this.factory.create(
                Objects.requireNonNull(context.targetCustomerId()),
                Objects.requireNonNull(context.projectId()),
                request.platform()
        );
    }
}
