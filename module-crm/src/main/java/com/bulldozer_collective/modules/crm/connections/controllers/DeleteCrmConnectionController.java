package com.bulldozer_collective.modules.crm.connections.controllers;

import com.bulldozer_collective.modules.crm.connections.security.DeleteCrmConnectionAccessChecker;
import com.bulldozer_collective.modules.crm.connections.services.CrmConnectionDeleter;
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
@RequestMapping("/crm/connections")
public class DeleteCrmConnectionController {

    private final CrmConnectionDeleter deleter;
    private final DeleteCrmConnectionAccessChecker accessChecker;

    @DeleteMapping("/{connectionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@JwtRequestContext RequestContext context,
                       @PathVariable @NonNull UUID connectionId) {
        this.accessChecker.checkAccess(context);
        this.deleter.delete(
                Objects.requireNonNull(context.targetCustomerId()),
                Objects.requireNonNull(context.projectId()),
                connectionId);
    }
}
