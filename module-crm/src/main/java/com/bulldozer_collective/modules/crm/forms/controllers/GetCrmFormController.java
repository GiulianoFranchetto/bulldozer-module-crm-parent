package com.bulldozer_collective.modules.crm.forms.controllers;

import com.bulldozer_collective.modules.crm.api.GetCrmFormResponse;
import com.bulldozer_collective.modules.crm.forms.security.GetCrmFormAccessChecker;
import com.bulldozer_collective.modules.crm.forms.services.CrmFormSearcher;
import com.bulldozer_collective.modules.crm.gateway.NoTokenException;
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
@RequestMapping("/crm/{connectionId}/forms")
public class GetCrmFormController {

    private final CrmFormSearcher searcher;
    private final GetCrmFormAccessChecker accessChecker;

    @GetMapping("/{id}")
    public GetCrmFormResponse get(@JwtRequestContext RequestContext context,
                                  @PathVariable @NonNull UUID connectionId,
                                  @PathVariable @NonNull String id) throws NoTokenException {
        this.accessChecker.checkAccess(context);
        return new GetCrmFormResponse(this.searcher.get(
                Objects.requireNonNull(context.targetCustomerId()),
                Objects.requireNonNull(context.projectId()),
                connectionId,
                id));
    }
}
