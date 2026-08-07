package com.bulldozer_collective.modules.crm.companies.controllers;

import com.bulldozer_collective.modules.crm.api.GetCrmCompanyResponse;
import com.bulldozer_collective.modules.crm.companies.security.GetCrmCompanyAccessChecker;
import com.bulldozer_collective.modules.crm.companies.services.CrmCompanyConverter;
import com.bulldozer_collective.modules.crm.companies.services.CrmCompanyFetcher;
import com.bulldozer_collective.security.annotations.JwtRequestContext;
import com.bulldozer_collective.security.resolvers.RequestContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/crm/companies")
public class GetCrmCompanyController {

    private final CrmCompanyFetcher fetcher;
    private final CrmCompanyConverter converter;
    private final GetCrmCompanyAccessChecker accessChecker;

    @GetMapping("/{id}")
    public GetCrmCompanyResponse get(@JwtRequestContext RequestContext context,
                                       @PathVariable UUID id) {
        this.accessChecker.checkAccess(context);
        // orElseThrow() raises NoSuchElementException, which the adviser maps to a 404.
        var entity = this.fetcher.findById(id,
                        Objects.requireNonNull(context.targetCustomerId()),
                        Objects.requireNonNull(context.projectId()))
                .orElseThrow();
        return new GetCrmCompanyResponse(this.converter.convert(entity));
    }
}
