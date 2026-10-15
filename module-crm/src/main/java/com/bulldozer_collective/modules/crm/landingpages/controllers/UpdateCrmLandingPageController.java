package com.bulldozer_collective.modules.crm.landingpages.controllers;

import com.bulldozer_collective.modules.crm.api.UpdateCrmLandingPageRequest;
import com.bulldozer_collective.modules.crm.api.UpdateCrmLandingPageResponse;
import com.bulldozer_collective.modules.crm.gateway.NoTokenException;
import com.bulldozer_collective.modules.crm.landingpages.security.UpdateCrmLandingPageAccessChecker;
import com.bulldozer_collective.modules.crm.landingpages.services.CrmLandingPageUpdater;
import com.bulldozer_collective.modules.crm.landingpages.services.SaveCrmLandingPageBuilder;
import com.bulldozer_collective.security.annotations.JwtRequestContext;
import com.bulldozer_collective.security.resolvers.RequestContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/crm/{connectionId}/landing-pages")
public class UpdateCrmLandingPageController {

    private final CrmLandingPageUpdater updater;
    private final UpdateCrmLandingPageAccessChecker accessChecker;

    @PatchMapping("/{id}")
    public UpdateCrmLandingPageResponse update(@JwtRequestContext RequestContext context,
                                               @PathVariable @NonNull UUID connectionId,
                                               @PathVariable @NonNull String id,
                                               @Valid @RequestBody @NonNull UpdateCrmLandingPageRequest request) throws NoTokenException {
        this.accessChecker.checkAccess(context);
        var page = this.updater.update(
                Objects.requireNonNull(context.targetCustomerId()),
                Objects.requireNonNull(context.projectId()),
                connectionId,
                id,
                SaveCrmLandingPageBuilder.builder()
                        .name(request.name())
                        .slug(request.slug())
                        .domain(request.domain())
                        .htmlTitle(request.htmlTitle())
                        .metaDescription(request.metaDescription())
                        .language(request.language())
                        .content(request.content())
                        .build());
        return new UpdateCrmLandingPageResponse(page);
    }
}
