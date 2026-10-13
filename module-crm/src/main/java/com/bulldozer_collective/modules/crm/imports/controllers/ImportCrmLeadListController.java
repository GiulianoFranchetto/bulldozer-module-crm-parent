package com.bulldozer_collective.modules.crm.imports.controllers;

import com.bulldozer_collective.modules.crm.api.ImportCrmLeadListRequest;
import com.bulldozer_collective.modules.crm.api.ImportCrmLeadListResponse;
import com.bulldozer_collective.modules.crm.gateway.NoTokenException;
import com.bulldozer_collective.modules.crm.imports.security.ImportCrmLeadListAccessChecker;
import com.bulldozer_collective.modules.crm.imports.services.CrmLeadListImporter;
import com.bulldozer_collective.security.annotations.JwtRequestContext;
import com.bulldozer_collective.security.resolvers.RequestContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/crm/{connectionId}/lead-list-imports")
public class ImportCrmLeadListController {

    private final CrmLeadListImporter importer;
    private final ImportCrmLeadListAccessChecker accessChecker;

    @PostMapping
    public ImportCrmLeadListResponse importLeadList(@JwtRequestContext RequestContext context,
                                                    @PathVariable @NonNull UUID connectionId,
                                                    @Valid @RequestBody @NonNull ImportCrmLeadListRequest request)
            throws NoTokenException {
        this.accessChecker.checkAccess(context);
        return this.importer.importLeadList(
                Objects.requireNonNull(context.targetCustomerId()),
                Objects.requireNonNull(context.projectId()),
                connectionId,
                request.leadListId(),
                request.legalBasis());
    }
}
