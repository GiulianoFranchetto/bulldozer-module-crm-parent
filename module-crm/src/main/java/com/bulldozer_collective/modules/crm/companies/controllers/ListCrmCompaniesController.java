package com.bulldozer_collective.modules.crm.companies.controllers;

import com.bulldozer_collective.modules.crm.api.CrmCompany;
import com.bulldozer_collective.modules.crm.api.ListCrmCompaniesParam;
import com.bulldozer_collective.modules.crm.companies.security.ListCrmCompaniesAccessChecker;
import com.bulldozer_collective.modules.crm.companies.services.CrmCompanyConverter;
import com.bulldozer_collective.modules.crm.companies.services.CrmCompanyFetcher;
import com.bulldozer_collective.modules.crm.companies.services.ListCrmCompaniesQuery;
import com.bulldozer_collective.security.annotations.JwtRequestContext;
import jakarta.validation.Valid;
import com.bulldozer_collective.security.resolvers.RequestContext;
import com.bulldozer_collective.shared.ResponsePage;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequiredArgsConstructor
@RequestMapping("/crm/companies")
public class ListCrmCompaniesController {

    private final CrmCompanyFetcher fetcher;
    private final CrmCompanyConverter converter;
    private final ListCrmCompaniesAccessChecker accessChecker;

    @GetMapping
    public ResponsePage<CrmCompany> list(@JwtRequestContext RequestContext context,
                                               @Valid ListCrmCompaniesParam param) {
        this.accessChecker.checkAccess(context);
        var page = this.fetcher.search(ListCrmCompaniesQuery.builder()
                .customerId(Objects.requireNonNull(context.targetCustomerId()))
                .projectId(Objects.requireNonNull(context.projectId()))
                .platform(param.platform())
                .q(param.q())
                .page(param.page())
                .size(param.size())
                .build());
        return this.converter.convert(page);
    }
}
