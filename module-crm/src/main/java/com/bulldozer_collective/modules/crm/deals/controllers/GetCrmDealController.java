package com.bulldozer_collective.modules.crm.deals.controllers;

import com.bulldozer_collective.modules.crm.api.CrmContainerParam;
import com.bulldozer_collective.modules.crm.api.GetCrmDealResponse;
import com.bulldozer_collective.modules.crm.deals.security.GetCrmDealAccessChecker;
import com.bulldozer_collective.modules.crm.deals.services.CrmDealSearcher;
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
@RequestMapping("/crm/{connectionId}/deals")
public class GetCrmDealController {

    private final CrmDealSearcher searcher;
    private final GetCrmDealAccessChecker accessChecker;

    @GetMapping("/{id}")
    public GetCrmDealResponse get(@JwtRequestContext RequestContext context,
                                  @PathVariable @NonNull UUID connectionId,
                                  @PathVariable @NonNull String id,
                                  @Valid @NonNull CrmContainerParam param) throws NoTokenException {
        this.accessChecker.checkAccess(context);
        return new GetCrmDealResponse(this.searcher.get(
                Objects.requireNonNull(context.targetCustomerId()),
                Objects.requireNonNull(context.projectId()),
                connectionId,
                param.containerId(),
                id));
    }
}
