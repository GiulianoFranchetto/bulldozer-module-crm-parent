package com.bulldozer_collective.modules.crm.activities.controllers;

import com.bulldozer_collective.modules.crm.activities.security.ListCrmActivitiesAccessChecker;
import com.bulldozer_collective.modules.crm.activities.services.CrmActivityConverter;
import com.bulldozer_collective.modules.crm.activities.services.CrmActivityFetcher;
import com.bulldozer_collective.modules.crm.api.CrmActivity;
import com.bulldozer_collective.modules.crm.api.ListCrmActivitiesParam;
import com.bulldozer_collective.modules.crm.activities.services.ListCrmActivitiesQuery;
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
@RequestMapping("/crm/activities")
public class ListCrmActivitiesController {

    private final CrmActivityFetcher fetcher;
    private final CrmActivityConverter converter;
    private final ListCrmActivitiesAccessChecker accessChecker;

    @GetMapping
    public ResponsePage<CrmActivity> list(@JwtRequestContext RequestContext context,
                                                @Valid ListCrmActivitiesParam param) {
        this.accessChecker.checkAccess(context);
        var page = this.fetcher.search(ListCrmActivitiesQuery.builder()
                .customerId(Objects.requireNonNull(context.targetCustomerId()))
                .projectId(Objects.requireNonNull(context.projectId()))
                .platform(param.platform())
                .type(param.type())
                .dealId(param.dealId())
                .contactId(param.contactId())
                .companyId(param.companyId())
                .page(param.page())
                .size(param.size())
                .build());
        return this.converter.convert(page);
    }
}
