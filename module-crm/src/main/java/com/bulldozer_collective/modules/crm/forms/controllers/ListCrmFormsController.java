package com.bulldozer_collective.modules.crm.forms.controllers;

import com.bulldozer_collective.modules.crm.api.CrmForm;
import com.bulldozer_collective.modules.crm.api.CrmPage;
import com.bulldozer_collective.modules.crm.api.ListCrmFormsParam;
import com.bulldozer_collective.modules.crm.forms.security.ListCrmFormsAccessChecker;
import com.bulldozer_collective.modules.crm.forms.services.CrmFormSearcher;
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
@RequestMapping("/crm/{connectionId}/forms")
public class ListCrmFormsController {

    private final CrmFormSearcher searcher;
    private final ListCrmFormsAccessChecker accessChecker;

    @GetMapping
    public CrmPage<CrmForm> list(@JwtRequestContext RequestContext context,
                                 @PathVariable @NonNull UUID connectionId,
                                 @Valid @NonNull ListCrmFormsParam param) throws NoTokenException {
        this.accessChecker.checkAccess(context);
        return this.searcher.list(
                Objects.requireNonNull(context.targetCustomerId()),
                Objects.requireNonNull(context.projectId()),
                connectionId,
                param.cursor(),
                Objects.requireNonNull(param.limit()));
    }
}
