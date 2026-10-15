package com.bulldozer_collective.modules.crm.landingpages.controllers;

import com.bulldozer_collective.modules.crm.api.GetCrmLandingPageResponse;
import com.bulldozer_collective.modules.crm.gateway.NoTokenException;
import com.bulldozer_collective.modules.crm.landingpages.security.GetCrmLandingPageAccessChecker;
import com.bulldozer_collective.modules.crm.landingpages.services.CrmLandingPageSearcher;
import com.bulldozer_collective.security.annotations.JwtRequestContext;
import com.bulldozer_collective.security.resolvers.RequestContext;
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
@RequestMapping("/crm/{connectionId}/landing-pages")
public class GetCrmLandingPageController {

    private final CrmLandingPageSearcher searcher;
    private final GetCrmLandingPageAccessChecker accessChecker;

    @GetMapping("/{id}")
    public GetCrmLandingPageResponse get(@JwtRequestContext RequestContext context,
                                         @PathVariable @NonNull UUID connectionId,
                                         @PathVariable @NonNull String id) throws NoTokenException {
        this.accessChecker.checkAccess(context);
        return new GetCrmLandingPageResponse(this.searcher.get(
                Objects.requireNonNull(context.targetCustomerId()),
                Objects.requireNonNull(context.projectId()),
                connectionId,
                id));
    }
}
