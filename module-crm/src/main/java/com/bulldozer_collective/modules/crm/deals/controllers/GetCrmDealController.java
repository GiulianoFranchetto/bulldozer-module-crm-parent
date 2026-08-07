package com.bulldozer_collective.modules.crm.deals.controllers;

import com.bulldozer_collective.modules.crm.api.GetCrmDealResponse;
import com.bulldozer_collective.modules.crm.deals.security.GetCrmDealAccessChecker;
import com.bulldozer_collective.modules.crm.deals.services.CrmDealConverter;
import com.bulldozer_collective.modules.crm.deals.services.CrmDealFetcher;
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
@RequestMapping("/crm/deals")
public class GetCrmDealController {

    private final CrmDealFetcher fetcher;
    private final CrmDealConverter converter;
    private final GetCrmDealAccessChecker accessChecker;

    @GetMapping("/{id}")
    public GetCrmDealResponse get(@JwtRequestContext RequestContext context,
                             @PathVariable UUID id) {
        this.accessChecker.checkAccess(context);
        // orElseThrow() raises NoSuchElementException, which the adviser maps to a 404.
        var entity = this.fetcher.findById(id,
                        Objects.requireNonNull(context.targetCustomerId()),
                        Objects.requireNonNull(context.projectId()))
                .orElseThrow();
        return new GetCrmDealResponse(this.converter.convert(entity));
    }
}
