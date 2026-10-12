package com.bulldozer_collective.modules.crm.deals.controllers;

import com.bulldozer_collective.modules.crm.api.CrmDeal;
import com.bulldozer_collective.modules.crm.api.CrmPage;
import com.bulldozer_collective.modules.crm.api.ListCrmDealsParam;
import com.bulldozer_collective.modules.crm.deals.security.ListCrmDealsAccessChecker;
import com.bulldozer_collective.modules.crm.deals.services.CrmDealSearcher;
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
@RequestMapping("/crm/{connectionId}/deals")
public class ListCrmDealsController {

    private final CrmDealSearcher searcher;
    private final ListCrmDealsAccessChecker accessChecker;

    @GetMapping
    public CrmPage<CrmDeal> list(@JwtRequestContext RequestContext context,
                                 @PathVariable @NonNull UUID connectionId,
                                 @Valid @NonNull ListCrmDealsParam param) throws NoTokenException {
        this.accessChecker.checkAccess(context);
        return this.searcher.list(
                Objects.requireNonNull(context.targetCustomerId()),
                Objects.requireNonNull(context.projectId()),
                connectionId,
                param.containerId(),
                ListCrmObjectsBuilder.builder()
                        .q(param.q())
                        .associatedTo(ListCrmObjectsBuilder.association(param.associatedType(), param.associatedId()))
                        .cursor(param.cursor())
                        .limit(Objects.requireNonNull(param.limit()))
                        .build());
    }
}
