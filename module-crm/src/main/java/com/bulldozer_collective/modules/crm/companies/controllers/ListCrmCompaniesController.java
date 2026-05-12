package com.bulldozer_collective.modules.crm.companies.controllers;

import com.bulldozer_collective.modules.crm.api.CrmCompanyRecord;
import com.bulldozer_collective.modules.crm.api.ListCrmCompaniesParam;
import com.bulldozer_collective.modules.crm.companies.security.ListCrmCompaniesAccessChecker;
import com.bulldozer_collective.modules.crm.companies.services.CrmCompanyConverter;
import com.bulldozer_collective.modules.crm.companies.services.CrmCompanyFetcher;
import com.bulldozer_collective.security.annotations.JwtRequestContext;
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
    public ResponsePage<CrmCompanyRecord> list(@JwtRequestContext RequestContext context,
                                               ListCrmCompaniesParam param) {
        this.accessChecker.checkAccess(context);
        var page = this.fetcher.search(
                Objects.requireNonNull(context.targetCustomerId()),
                Objects.requireNonNull(context.projectId()),
                param.platform(),
                param.q(),
                param.page(),
                param.size()
        );
        return this.converter.convert(page);
    }
}
