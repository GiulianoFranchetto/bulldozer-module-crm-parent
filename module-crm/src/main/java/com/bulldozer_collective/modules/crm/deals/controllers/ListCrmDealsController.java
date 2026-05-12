package com.bulldozer_collective.modules.crm.deals.controllers;

import com.bulldozer_collective.modules.crm.api.CrmDealRecord;
import com.bulldozer_collective.modules.crm.api.ListCrmDealsParam;
import com.bulldozer_collective.modules.crm.deals.security.ListCrmDealsAccessChecker;
import com.bulldozer_collective.modules.crm.deals.services.CrmDealConverter;
import com.bulldozer_collective.modules.crm.deals.services.CrmDealFetcher;
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
@RequestMapping("/crm/deals")
public class ListCrmDealsController {

    private final CrmDealFetcher fetcher;
    private final CrmDealConverter converter;
    private final ListCrmDealsAccessChecker accessChecker;

    @GetMapping
    public ResponsePage<CrmDealRecord> list(@JwtRequestContext RequestContext context,
                                            ListCrmDealsParam param) {
        this.accessChecker.checkAccess(context);
        var page = this.fetcher.search(
                Objects.requireNonNull(context.targetCustomerId()),
                Objects.requireNonNull(context.projectId()),
                param.platform(),
                param.internalStage(),
                param.pipelineId(),
                param.page(),
                param.size()
        );
        return this.converter.convert(page);
    }
}
