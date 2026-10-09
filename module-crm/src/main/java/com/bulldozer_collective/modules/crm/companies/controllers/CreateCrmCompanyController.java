package com.bulldozer_collective.modules.crm.companies.controllers;

import com.bulldozer_collective.modules.crm.api.CreateCrmCompanyRequest;
import com.bulldozer_collective.modules.crm.api.CreateCrmCompanyResponse;
import com.bulldozer_collective.modules.crm.companies.security.CreateCrmCompanyAccessChecker;
import com.bulldozer_collective.modules.crm.companies.services.CrmCompanyFactory;
import com.bulldozer_collective.modules.crm.companies.services.SaveCrmCompanyBuilder;
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
@RequestMapping("/crm/{connectionId}/companies")
public class CreateCrmCompanyController {

    private final CrmCompanyFactory factory;
    private final CreateCrmCompanyAccessChecker accessChecker;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateCrmCompanyResponse create(@JwtRequestContext RequestContext context,
                                           @PathVariable @NonNull UUID connectionId,
                                           @Valid @RequestBody @NonNull CreateCrmCompanyRequest request) throws NoTokenException {
        this.accessChecker.checkAccess(context);
        var company = this.factory.create(
                Objects.requireNonNull(context.targetCustomerId()),
                Objects.requireNonNull(context.projectId()),
                connectionId,
                SaveCrmCompanyBuilder.builder()
                        .name(request.name())
                        .domain(request.domain())
                        .industry(request.industry())
                        .phone(request.phone())
                        .city(request.city())
                        .country(request.country())
                        .ownerId(request.ownerId())
                        .build());
        return new CreateCrmCompanyResponse(company);
    }
}
