package com.bulldozer_collective.modules.crm.leads.controllers;

import com.bulldozer_collective.modules.crm.api.CreateCrmLeadRequest;
import com.bulldozer_collective.modules.crm.api.CreateCrmLeadResponse;
import com.bulldozer_collective.modules.crm.api.CrmContainerParam;
import com.bulldozer_collective.modules.crm.gateway.NoTokenException;
import com.bulldozer_collective.modules.crm.leads.security.CreateCrmLeadAccessChecker;
import com.bulldozer_collective.modules.crm.leads.services.CreateCrmLeadBuilder;
import com.bulldozer_collective.modules.crm.leads.services.CrmLeadFactory;
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
@RequestMapping("/crm/{connectionId}/leads")
public class CreateCrmLeadController {

    private final CrmLeadFactory factory;
    private final CreateCrmLeadAccessChecker accessChecker;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateCrmLeadResponse create(@JwtRequestContext RequestContext context,
                                        @PathVariable @NonNull UUID connectionId,
                                        @Valid @RequestBody @NonNull CreateCrmLeadRequest request,
                                        @Valid @NonNull CrmContainerParam param) throws NoTokenException {
        this.accessChecker.checkAccess(context);
        var lead = this.factory.create(
                Objects.requireNonNull(context.targetCustomerId()),
                Objects.requireNonNull(context.projectId()),
                connectionId,
                param.containerId(),
                CreateCrmLeadBuilder.builder()
                        .name(request.name())
                        .contactId(request.contactId())
                        .stage(request.stage())
                        .type(request.type())
                        .build());
        return new CreateCrmLeadResponse(lead);
    }
}
