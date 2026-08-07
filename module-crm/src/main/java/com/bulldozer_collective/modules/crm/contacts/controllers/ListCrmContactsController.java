package com.bulldozer_collective.modules.crm.contacts.controllers;

import com.bulldozer_collective.modules.crm.api.ListCrmContactsParam;
import com.bulldozer_collective.modules.crm.contacts.security.ListCrmContactsAccessChecker;
import com.bulldozer_collective.modules.crm.contacts.services.CrmContactConverter;
import com.bulldozer_collective.modules.crm.contacts.services.CrmContactFetcher;
import com.bulldozer_collective.modules.crm.contacts.services.ListCrmContactsQuery;
import com.bulldozer_collective.security.annotations.JwtRequestContext;
import jakarta.validation.Valid;
import com.bulldozer_collective.security.resolvers.RequestContext;
import com.bulldozer_collective.shared.ResponsePage;
import com.bulldozer_collective.modules.crm.api.CrmContact;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequiredArgsConstructor
@RequestMapping("/crm/contacts")
public class ListCrmContactsController {

    private final CrmContactFetcher fetcher;
    private final CrmContactConverter converter;
    private final ListCrmContactsAccessChecker accessChecker;

    @GetMapping
    public ResponsePage<CrmContact> list(@JwtRequestContext RequestContext context,
                                               @Valid ListCrmContactsParam param) {
        this.accessChecker.checkAccess(context);
        var page = this.fetcher.search(ListCrmContactsQuery.builder()
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
