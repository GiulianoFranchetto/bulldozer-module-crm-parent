package com.bulldozer_collective.modules.crm.landingpages.controllers;

import com.bulldozer_collective.modules.crm.api.CreateCrmLandingPageRequest;
import com.bulldozer_collective.modules.crm.api.CreateCrmLandingPageResponse;
import com.bulldozer_collective.modules.crm.gateway.NoTokenException;
import com.bulldozer_collective.modules.crm.landingpages.security.CreateCrmLandingPageAccessChecker;
import com.bulldozer_collective.modules.crm.landingpages.services.CrmLandingPageFactory;
import com.bulldozer_collective.modules.crm.landingpages.services.SaveCrmLandingPageBuilder;
import com.bulldozer_collective.security.annotations.JwtRequestContext;
import com.bulldozer_collective.security.resolvers.RequestContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/crm/{connectionId}/landing-pages")
public class CreateCrmLandingPageController {

    private final CrmLandingPageFactory factory;
    private final CreateCrmLandingPageAccessChecker accessChecker;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateCrmLandingPageResponse create(@JwtRequestContext RequestContext context,
                                               @PathVariable @NonNull UUID connectionId,
                                               @Valid @RequestBody @NonNull CreateCrmLandingPageRequest request) throws NoTokenException {
        this.accessChecker.checkAccess(context);
        var page = this.factory.create(
                Objects.requireNonNull(context.targetCustomerId()),
                Objects.requireNonNull(context.projectId()),
                connectionId,
                SaveCrmLandingPageBuilder.builder()
                        .name(request.name())
                        .templatePath(request.templatePath())
                        .slug(request.slug())
                        .domain(request.domain())
                        .htmlTitle(request.htmlTitle())
                        .metaDescription(request.metaDescription())
                        .language(request.language())
                        .content(request.content())
                        .build());
        return new CreateCrmLandingPageResponse(page);
    }
}
