package com.bulldozer_collective.modules.crm.sync.controllers;

import com.bulldozer_collective.modules.crm.api.CrmSyncRunStartedResponse;
import com.bulldozer_collective.modules.crm.api.TriggerCrmSyncRequest;
import com.bulldozer_collective.modules.crm.sync.security.TriggerCrmSyncAccessChecker;
import com.bulldozer_collective.modules.crm.sync.services.CrmSyncOrchestrator;
import com.bulldozer_collective.security.annotations.JwtRequestContext;
import com.bulldozer_collective.security.resolvers.RequestContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequiredArgsConstructor
@RequestMapping("/crm/sync")
public class TriggerCrmSyncController {

    private final CrmSyncOrchestrator orchestrator;
    private final TriggerCrmSyncAccessChecker accessChecker;

    @PostMapping("/trigger")
    public CrmSyncRunStartedResponse trigger(@JwtRequestContext RequestContext context,
                                             @Valid @RequestBody TriggerCrmSyncRequest request) {
        this.accessChecker.checkAccess(context);
        var run = this.orchestrator.enqueue(
                Objects.requireNonNull(context.targetCustomerId()),
                Objects.requireNonNull(context.projectId()),
                request.platform()
        );
        return new CrmSyncRunStartedResponse(run.getId());
    }
}
