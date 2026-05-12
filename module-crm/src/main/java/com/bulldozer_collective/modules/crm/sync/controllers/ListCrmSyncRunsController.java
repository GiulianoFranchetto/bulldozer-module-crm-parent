package com.bulldozer_collective.modules.crm.sync.controllers;

import com.bulldozer_collective.modules.crm.api.CrmSyncRunRecord;
import com.bulldozer_collective.modules.crm.api.ListCrmSyncRunsParam;
import com.bulldozer_collective.modules.crm.sync.security.ListCrmSyncRunsAccessChecker;
import com.bulldozer_collective.modules.crm.sync.services.CrmSyncRunConverter;
import com.bulldozer_collective.modules.crm.sync.services.CrmSyncRunFetcher;
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
@RequestMapping("/crm/sync")
public class ListCrmSyncRunsController {

    private final CrmSyncRunFetcher fetcher;
    private final CrmSyncRunConverter converter;
    private final ListCrmSyncRunsAccessChecker accessChecker;

    @GetMapping("/runs")
    public ResponsePage<CrmSyncRunRecord> list(@JwtRequestContext RequestContext context,
                                               ListCrmSyncRunsParam param) {
        this.accessChecker.checkAccess(context);
        var page = this.fetcher.search(
                Objects.requireNonNull(context.targetCustomerId()),
                Objects.requireNonNull(context.projectId()),
                param.platform(),
                param.status(),
                param.page(),
                param.size()
        );
        return this.converter.convert(page);
    }
}
