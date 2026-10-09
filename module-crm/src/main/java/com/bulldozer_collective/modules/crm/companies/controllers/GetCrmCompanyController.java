package com.bulldozer_collective.modules.crm.companies.controllers;

import com.bulldozer_collective.modules.crm.api.GetCrmCompanyResponse;
import com.bulldozer_collective.modules.crm.companies.security.GetCrmCompanyAccessChecker;
import com.bulldozer_collective.modules.crm.companies.services.CrmCompanySearcher;
import com.bulldozer_collective.modules.crm.gateway.NoTokenException;
import com.bulldozer_collective.security.annotations.JwtRequestContext;
import com.bulldozer_collective.security.resolvers.RequestContext;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/crm/{connectionId}/companies")
public class GetCrmCompanyController {

    private final CrmCompanySearcher searcher;
    private final GetCrmCompanyAccessChecker accessChecker;

    @GetMapping("/{id}")
    public GetCrmCompanyResponse get(@JwtRequestContext RequestContext context,
                                     @PathVariable @NonNull UUID connectionId,
                                     @PathVariable @NonNull String id) throws NoTokenException {
        this.accessChecker.checkAccess(context);
        return new GetCrmCompanyResponse(this.searcher.get(
                Objects.requireNonNull(context.targetCustomerId()),
                Objects.requireNonNull(context.projectId()),
                connectionId,
                id));
    }
}
