package com.bulldozer_collective.modules.crm.landingpages.controllers;

import com.bulldozer_collective.modules.crm.api.PublishCrmLandingPageRequest;
import com.bulldozer_collective.modules.crm.api.PublishCrmLandingPageResponse;
import com.bulldozer_collective.modules.crm.gateway.NoTokenException;
import com.bulldozer_collective.modules.crm.landingpages.security.PublishCrmLandingPageAccessChecker;
import com.bulldozer_collective.modules.crm.landingpages.services.CrmLandingPageUpdater;
import com.bulldozer_collective.security.annotations.JwtRequestContext;
import com.bulldozer_collective.security.resolvers.RequestContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/crm/{connectionId}/landing-pages")
public class PublishCrmLandingPageController {

    private final CrmLandingPageUpdater updater;
    private final PublishCrmLandingPageAccessChecker accessChecker;

    /** The body is optional: without one, the page goes live now. */
    @PostMapping("/{id}/publish")
    public PublishCrmLandingPageResponse publish(@JwtRequestContext RequestContext context,
                                                @PathVariable @NonNull UUID connectionId,
                                                @PathVariable @NonNull String id,
                                                @Valid @RequestBody(required = false) @Nullable PublishCrmLandingPageRequest request) throws NoTokenException {
        this.accessChecker.checkAccess(context);
        var page = this.updater.publish(
                Objects.requireNonNull(context.targetCustomerId()),
                Objects.requireNonNull(context.projectId()),
                connectionId,
                id,
                request == null ? null : request.publishAt());
        return new PublishCrmLandingPageResponse(page);
    }
}
