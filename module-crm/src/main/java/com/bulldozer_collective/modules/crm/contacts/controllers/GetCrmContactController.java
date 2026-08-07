package com.bulldozer_collective.modules.crm.contacts.controllers;

import com.bulldozer_collective.modules.crm.api.GetCrmContactResponse;
import com.bulldozer_collective.modules.crm.contacts.security.GetCrmContactAccessChecker;
import com.bulldozer_collective.modules.crm.contacts.services.CrmContactConverter;
import com.bulldozer_collective.modules.crm.contacts.services.CrmContactFetcher;
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
@RequestMapping("/crm/contacts")
public class GetCrmContactController {

    private final CrmContactFetcher fetcher;
    private final CrmContactConverter converter;
    private final GetCrmContactAccessChecker accessChecker;

    @GetMapping("/{id}")
    public GetCrmContactResponse get(@JwtRequestContext RequestContext context,
                                       @PathVariable UUID id) {
        this.accessChecker.checkAccess(context);
        // orElseThrow() raises NoSuchElementException, which the adviser maps to a 404.
        var entity = this.fetcher.findById(id,
                        Objects.requireNonNull(context.targetCustomerId()),
                        Objects.requireNonNull(context.projectId()))
                .orElseThrow();
        return new GetCrmContactResponse(this.converter.convert(entity));
    }
}
