package com.bulldozer_collective.modules.crm.activities.controllers;

import com.bulldozer_collective.modules.crm.activities.security.GetCrmActivityAccessChecker;
import com.bulldozer_collective.modules.crm.activities.services.CrmActivitySearcher;
import com.bulldozer_collective.modules.crm.api.GetCrmActivityParam;
import com.bulldozer_collective.modules.crm.api.GetCrmActivityResponse;
import com.bulldozer_collective.modules.crm.gateway.NoTokenException;
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
@RequestMapping("/crm/{connectionId}/activities")
public class GetCrmActivityController {

    private final CrmActivitySearcher searcher;
    private final GetCrmActivityAccessChecker accessChecker;

    @GetMapping("/{id}")
    public GetCrmActivityResponse get(@JwtRequestContext RequestContext context,
                                      @PathVariable @NonNull UUID connectionId,
                                      @PathVariable @NonNull String id,
                                      @Valid @NonNull GetCrmActivityParam param) throws NoTokenException {
        this.accessChecker.checkAccess(context);
        return new GetCrmActivityResponse(this.searcher.get(
                Objects.requireNonNull(context.targetCustomerId()),
                Objects.requireNonNull(context.projectId()),
                connectionId,
                param.type(),
                id));
    }
}
