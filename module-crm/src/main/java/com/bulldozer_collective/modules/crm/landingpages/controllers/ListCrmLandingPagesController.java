package com.bulldozer_collective.modules.crm.landingpages.controllers;

import com.bulldozer_collective.modules.crm.api.CrmLandingPage;
import com.bulldozer_collective.modules.crm.api.CrmPage;
import com.bulldozer_collective.modules.crm.api.ListCrmLandingPagesParam;
import com.bulldozer_collective.modules.crm.gateway.NoTokenException;
import com.bulldozer_collective.modules.crm.landingpages.security.ListCrmLandingPagesAccessChecker;
import com.bulldozer_collective.modules.crm.landingpages.services.CrmLandingPageSearcher;
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
@RequestMapping("/crm/{connectionId}/landing-pages")
public class ListCrmLandingPagesController {

    private final CrmLandingPageSearcher searcher;
    private final ListCrmLandingPagesAccessChecker accessChecker;

    @GetMapping
    public CrmPage<CrmLandingPage> list(@JwtRequestContext RequestContext context,
                                        @PathVariable @NonNull UUID connectionId,
                                        @Valid @NonNull ListCrmLandingPagesParam param) throws NoTokenException {
        this.accessChecker.checkAccess(context);
        return this.searcher.list(
                Objects.requireNonNull(context.targetCustomerId()),
                Objects.requireNonNull(context.projectId()),
                connectionId,
                param.cursor(),
                Objects.requireNonNull(param.limit()));
    }
}
