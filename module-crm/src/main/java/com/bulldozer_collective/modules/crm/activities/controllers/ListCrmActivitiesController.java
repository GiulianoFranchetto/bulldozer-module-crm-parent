package com.bulldozer_collective.modules.crm.activities.controllers;

import com.bulldozer_collective.modules.crm.activities.security.ListCrmActivitiesAccessChecker;
import com.bulldozer_collective.modules.crm.activities.services.CrmActivityConverter;
import com.bulldozer_collective.modules.crm.activities.services.CrmActivityFetcher;
import com.bulldozer_collective.modules.crm.api.CrmActivityRecord;
import com.bulldozer_collective.modules.crm.api.ListCrmActivitiesParam;
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
@RequestMapping("/crm/activities")
public class ListCrmActivitiesController {

    private final CrmActivityFetcher fetcher;
    private final CrmActivityConverter converter;
    private final ListCrmActivitiesAccessChecker accessChecker;

    @GetMapping
    public ResponsePage<CrmActivityRecord> list(@JwtRequestContext RequestContext context,
                                                ListCrmActivitiesParam param) {
        this.accessChecker.checkAccess(context);
        var page = this.fetcher.search(
                Objects.requireNonNull(context.targetCustomerId()),
                Objects.requireNonNull(context.projectId()),
                param.platform(),
                param.type(),
                param.dealId(),
                param.contactId(),
                param.companyId(),
                param.page(),
                param.size()
        );
        return this.converter.convert(page);
    }
}
