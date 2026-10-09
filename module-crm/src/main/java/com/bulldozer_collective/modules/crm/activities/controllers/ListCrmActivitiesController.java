package com.bulldozer_collective.modules.crm.activities.controllers;

import com.bulldozer_collective.modules.crm.activities.security.ListCrmActivitiesAccessChecker;
import com.bulldozer_collective.modules.crm.activities.services.CrmActivitySearcher;
import com.bulldozer_collective.modules.crm.api.CrmActivity;
import com.bulldozer_collective.modules.crm.api.CrmPage;
import com.bulldozer_collective.modules.crm.api.ListCrmActivitiesParam;
import com.bulldozer_collective.modules.crm.gateway.ListCrmObjectsBuilder;
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
public class ListCrmActivitiesController {

    private final CrmActivitySearcher searcher;
    private final ListCrmActivitiesAccessChecker accessChecker;

    @GetMapping
    public CrmPage<CrmActivity> list(@JwtRequestContext RequestContext context,
                                     @PathVariable @NonNull UUID connectionId,
                                     @Valid @NonNull ListCrmActivitiesParam param) throws NoTokenException {
        this.accessChecker.checkAccess(context);
        return this.searcher.list(
                Objects.requireNonNull(context.targetCustomerId()),
                Objects.requireNonNull(context.projectId()),
                connectionId,
                param.type(),
                ListCrmObjectsBuilder.builder()
                        .q(param.q())
                        .associatedTo(ListCrmObjectsBuilder.association(param.associatedType(), param.associatedId()))
                        .cursor(param.cursor())
                        .limit(Objects.requireNonNull(param.limit()))
                        .build());
    }
}
