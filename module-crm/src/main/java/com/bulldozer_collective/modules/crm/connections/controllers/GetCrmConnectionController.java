package com.bulldozer_collective.modules.crm.connections.controllers;

import com.bulldozer_collective.modules.crm.api.GetCrmConnectionResponse;
import com.bulldozer_collective.modules.crm.connections.security.GetCrmConnectionAccessChecker;
import com.bulldozer_collective.modules.crm.connections.services.CrmConnectionConverter;
import com.bulldozer_collective.modules.crm.connections.services.CrmConnectionFetcher;
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
@RequestMapping("/crm/connections")
public class GetCrmConnectionController {

    private final CrmConnectionFetcher fetcher;
    private final CrmConnectionConverter converter;
    private final GetCrmConnectionAccessChecker accessChecker;

    @GetMapping("/{connectionId}")
    public GetCrmConnectionResponse get(@JwtRequestContext RequestContext context,
                                        @PathVariable @NonNull UUID connectionId) {
        this.accessChecker.checkAccess(context);
        var connection = this.fetcher.fetch(
                Objects.requireNonNull(context.targetCustomerId()),
                Objects.requireNonNull(context.projectId()),
                connectionId);
        return new GetCrmConnectionResponse(this.converter.convert(connection));
    }
}
