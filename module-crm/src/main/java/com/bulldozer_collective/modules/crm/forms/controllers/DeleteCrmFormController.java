package com.bulldozer_collective.modules.crm.forms.controllers;

import com.bulldozer_collective.modules.crm.forms.security.DeleteCrmFormAccessChecker;
import com.bulldozer_collective.modules.crm.forms.services.CrmFormDeleter;
import com.bulldozer_collective.modules.crm.gateway.NoTokenException;
import com.bulldozer_collective.security.annotations.JwtRequestContext;
import com.bulldozer_collective.security.resolvers.RequestContext;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/crm/{connectionId}/forms")
public class DeleteCrmFormController {

    private final CrmFormDeleter deleter;
    private final DeleteCrmFormAccessChecker accessChecker;

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@JwtRequestContext RequestContext context,
                       @PathVariable @NonNull UUID connectionId,
                       @PathVariable @NonNull String id) throws NoTokenException {
        this.accessChecker.checkAccess(context);
        this.deleter.delete(
                Objects.requireNonNull(context.targetCustomerId()),
                Objects.requireNonNull(context.projectId()),
                connectionId,
                id);
    }
}
