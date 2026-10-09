package com.bulldozer_collective.modules.crm.companies.controllers;

import com.bulldozer_collective.modules.crm.api.CrmCompany;
import com.bulldozer_collective.modules.crm.api.CrmPage;
import com.bulldozer_collective.modules.crm.api.ListCrmCompaniesParam;
import com.bulldozer_collective.modules.crm.companies.security.ListCrmCompaniesAccessChecker;
import com.bulldozer_collective.modules.crm.companies.services.CrmCompanySearcher;
import com.bulldozer_collective.modules.crm.gateway.ListCrmObjectsBuilder;
import com.bulldozer_collective.modules.crm.gateway.NoTokenException;
import com.bulldozer_collective.security.annotations.JwtRequestContext;
import com.bulldozer_collective.security.resolvers.RequestContext;
import jakarta.validation.Valid;
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
public class ListCrmCompaniesController {

    private final CrmCompanySearcher searcher;
    private final ListCrmCompaniesAccessChecker accessChecker;

    @GetMapping
    public CrmPage<CrmCompany> list(@JwtRequestContext RequestContext context,
                                    @PathVariable @NonNull UUID connectionId,
                                    @Valid @NonNull ListCrmCompaniesParam param) throws NoTokenException {
        this.accessChecker.checkAccess(context);
        return this.searcher.list(
                Objects.requireNonNull(context.targetCustomerId()),
                Objects.requireNonNull(context.projectId()),
                connectionId,
                ListCrmObjectsBuilder.builder()
                        .q(param.q())
                        .associatedTo(ListCrmObjectsBuilder.association(param.associatedType(), param.associatedId()))
                        .cursor(param.cursor())
                        .limit(Objects.requireNonNull(param.limit()))
                        .build());
    }
}
