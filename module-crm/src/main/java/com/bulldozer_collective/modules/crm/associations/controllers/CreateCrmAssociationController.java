package com.bulldozer_collective.modules.crm.associations.controllers;

import com.bulldozer_collective.modules.crm.api.CrmAssociationParam;
import com.bulldozer_collective.modules.crm.api.CrmObjectRef;
import com.bulldozer_collective.modules.crm.associations.security.CreateCrmAssociationAccessChecker;
import com.bulldozer_collective.modules.crm.associations.services.CrmAssociationFactory;
import com.bulldozer_collective.modules.crm.gateway.NoTokenException;
import com.bulldozer_collective.security.annotations.JwtRequestContext;
import com.bulldozer_collective.security.resolvers.RequestContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/crm/{connectionId}/associations")
public class CreateCrmAssociationController {

    private final CrmAssociationFactory factory;
    private final CreateCrmAssociationAccessChecker accessChecker;

    @PutMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void associate(@JwtRequestContext RequestContext context,
                          @PathVariable @NonNull UUID connectionId,
                          @Valid @NonNull CrmAssociationParam param) throws NoTokenException {
        this.accessChecker.checkAccess(context);
        this.factory.associate(
                Objects.requireNonNull(context.targetCustomerId()),
                Objects.requireNonNull(context.projectId()),
                connectionId,
                new CrmObjectRef(param.fromType(), param.fromId()),
                new CrmObjectRef(param.toType(), param.toId()));
    }
}
