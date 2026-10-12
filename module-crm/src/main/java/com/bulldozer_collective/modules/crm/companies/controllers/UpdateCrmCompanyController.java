package com.bulldozer_collective.modules.crm.companies.controllers;

import com.bulldozer_collective.modules.crm.api.UpdateCrmCompanyRequest;
import com.bulldozer_collective.modules.crm.api.UpdateCrmCompanyResponse;
import com.bulldozer_collective.modules.crm.companies.security.UpdateCrmCompanyAccessChecker;
import com.bulldozer_collective.modules.crm.companies.services.CrmCompanyUpdater;
import com.bulldozer_collective.modules.crm.companies.services.SaveCrmCompanyBuilder;
import com.bulldozer_collective.modules.crm.gateway.NoTokenException;
import com.bulldozer_collective.security.annotations.JwtRequestContext;
import com.bulldozer_collective.security.resolvers.RequestContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/crm/{connectionId}/companies")
public class UpdateCrmCompanyController {

    private final CrmCompanyUpdater updater;
    private final UpdateCrmCompanyAccessChecker accessChecker;

    @PatchMapping("/{id}")
    public UpdateCrmCompanyResponse update(@JwtRequestContext RequestContext context,
                                           @PathVariable @NonNull UUID connectionId,
                                           @Valid @RequestBody @NonNull UpdateCrmCompanyRequest request,
                                           @PathVariable @NonNull String id) throws NoTokenException {
        this.accessChecker.checkAccess(context);
        var company = this.updater.update(
                Objects.requireNonNull(context.targetCustomerId()),
                Objects.requireNonNull(context.projectId()),
                connectionId,
                id,
                SaveCrmCompanyBuilder.builder()
                        .name(request.name())
                        .domain(request.domain())
                        .industry(request.industry())
                        .phone(request.phone())
                        .city(request.city())
                        .country(request.country())
                        .build());
        return new UpdateCrmCompanyResponse(company);
    }
}
