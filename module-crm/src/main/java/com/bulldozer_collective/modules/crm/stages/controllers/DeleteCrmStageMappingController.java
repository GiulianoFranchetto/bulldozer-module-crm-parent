package com.bulldozer_collective.modules.crm.stages.controllers;

import com.bulldozer_collective.modules.crm.stages.security.DeleteCrmStageMappingAccessChecker;
import com.bulldozer_collective.modules.crm.stages.services.CrmStageMappingDeleter;
import com.bulldozer_collective.security.annotations.JwtRequestContext;
import com.bulldozer_collective.security.resolvers.RequestContext;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/crm/{connectionId}/pipelines/{pipelineId}/stage-mappings")
public class DeleteCrmStageMappingController {

    private final CrmStageMappingDeleter deleter;
    private final DeleteCrmStageMappingAccessChecker accessChecker;

    @DeleteMapping("/{mappingId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@JwtRequestContext RequestContext context,
                       @PathVariable @NonNull UUID connectionId,
                       @PathVariable @NonNull String pipelineId,
                       @PathVariable @NonNull UUID mappingId) {
        this.accessChecker.checkAccess(context);
        this.deleter.delete(
                Objects.requireNonNull(context.targetCustomerId()),
                Objects.requireNonNull(context.projectId()),
                connectionId,
                pipelineId,
                mappingId);
    }
}
