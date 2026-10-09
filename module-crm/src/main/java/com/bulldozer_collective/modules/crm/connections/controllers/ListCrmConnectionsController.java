package com.bulldozer_collective.modules.crm.connections.controllers;

import com.bulldozer_collective.modules.crm.api.ListCrmConnectionsResponse;
import com.bulldozer_collective.modules.crm.connections.security.ListCrmConnectionsAccessChecker;
import com.bulldozer_collective.modules.crm.connections.services.CrmConnectionConverter;
import com.bulldozer_collective.modules.crm.connections.services.CrmConnectionFetcher;
import com.bulldozer_collective.security.annotations.JwtRequestContext;
import com.bulldozer_collective.security.resolvers.RequestContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequiredArgsConstructor
@RequestMapping("/crm/connections")
public class ListCrmConnectionsController {

    private final CrmConnectionFetcher fetcher;
    private final CrmConnectionConverter converter;
    private final ListCrmConnectionsAccessChecker accessChecker;

    @GetMapping
    public ListCrmConnectionsResponse list(@JwtRequestContext RequestContext context) {
        this.accessChecker.checkAccess(context);
        var connections = this.fetcher.fetchAll(
                Objects.requireNonNull(context.targetCustomerId()),
                Objects.requireNonNull(context.projectId()));
        return new ListCrmConnectionsResponse(this.converter.convert(connections));
    }
}
