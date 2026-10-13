package com.bulldozer_collective.modules.crm.containers.controllers;

import com.bulldozer_collective.modules.crm.api.ListCrmPropertiesParam;
import com.bulldozer_collective.modules.crm.api.ListCrmPropertiesResponse;
import com.bulldozer_collective.modules.crm.containers.security.ListCrmPropertiesAccessChecker;
import com.bulldozer_collective.modules.crm.containers.services.CrmContainerSearcher;
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
@RequestMapping("/crm/{connectionId}/properties")
public class ListCrmPropertiesController {

    private final CrmContainerSearcher searcher;
    private final ListCrmPropertiesAccessChecker accessChecker;

    @GetMapping
    public ListCrmPropertiesResponse list(@JwtRequestContext RequestContext context,
                                          @PathVariable @NonNull UUID connectionId,
                                          @Valid @NonNull ListCrmPropertiesParam param) throws NoTokenException {
        this.accessChecker.checkAccess(context);
        return new ListCrmPropertiesResponse(this.searcher.listProperties(
                Objects.requireNonNull(context.targetCustomerId()),
                Objects.requireNonNull(context.projectId()),
                connectionId,
                param.type(),
                param.name(),
                Boolean.TRUE.equals(param.selectable())));
    }
}
