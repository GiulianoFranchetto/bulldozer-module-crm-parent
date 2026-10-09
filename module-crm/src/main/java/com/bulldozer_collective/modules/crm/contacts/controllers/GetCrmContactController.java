package com.bulldozer_collective.modules.crm.contacts.controllers;

import com.bulldozer_collective.modules.crm.api.GetCrmContactResponse;
import com.bulldozer_collective.modules.crm.contacts.security.GetCrmContactAccessChecker;
import com.bulldozer_collective.modules.crm.contacts.services.CrmContactSearcher;
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
@RequestMapping("/crm/{connectionId}/contacts")
public class GetCrmContactController {

    private final CrmContactSearcher searcher;
    private final GetCrmContactAccessChecker accessChecker;

    @GetMapping("/{id}")
    public GetCrmContactResponse get(@JwtRequestContext RequestContext context,
                                     @PathVariable @NonNull UUID connectionId,
                                     @PathVariable @NonNull String id) throws NoTokenException {
        this.accessChecker.checkAccess(context);
        return new GetCrmContactResponse(this.searcher.get(
                Objects.requireNonNull(context.targetCustomerId()),
                Objects.requireNonNull(context.projectId()),
                connectionId,
                id));
    }
}
