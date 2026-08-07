package com.bulldozer_collective.modules.crm.activities.controllers;

import com.bulldozer_collective.modules.crm.activities.security.GetCrmActivityAccessChecker;
import com.bulldozer_collective.modules.crm.activities.services.CrmActivityConverter;
import com.bulldozer_collective.modules.crm.activities.services.CrmActivityFetcher;
import com.bulldozer_collective.modules.crm.api.GetCrmActivityResponse;
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
@RequestMapping("/crm/activities")
public class GetCrmActivityController {

    private final CrmActivityFetcher fetcher;
    private final CrmActivityConverter converter;
    private final GetCrmActivityAccessChecker accessChecker;

    @GetMapping("/{id}")
    public GetCrmActivityResponse get(@JwtRequestContext RequestContext context,
                                 @PathVariable UUID id) {
        this.accessChecker.checkAccess(context);
        // orElseThrow() raises NoSuchElementException, which the adviser maps to a 404.
        var entity = this.fetcher.findById(id,
                        Objects.requireNonNull(context.targetCustomerId()),
                        Objects.requireNonNull(context.projectId()))
                .orElseThrow();
        return new GetCrmActivityResponse(this.converter.convert(entity));
    }
}
