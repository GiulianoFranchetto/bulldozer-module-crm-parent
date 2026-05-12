package com.bulldozer_collective.modules.crm.pipelines.controllers;

import com.bulldozer_collective.modules.crm.pipelines.security.DeleteCrmStageMappingAccessChecker;
import com.bulldozer_collective.modules.crm.pipelines.services.CrmStageMappingDeleter;
import com.bulldozer_collective.security.annotations.JwtRequestContext;
import com.bulldozer_collective.security.resolvers.RequestContext;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("/crm/stage-mappings")
public class DeleteCrmStageMappingController {

    private final CrmStageMappingDeleter deleter;
    private final DeleteCrmStageMappingAccessChecker accessChecker;

    @DeleteMapping("/{mappingId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@JwtRequestContext RequestContext context,
                       @PathVariable UUID mappingId) {
        this.accessChecker.checkAccess(context);
        this.deleter.delete(
                mappingId,
                Objects.requireNonNull(context.targetCustomerId()),
                Objects.requireNonNull(context.projectId())
        );
    }
}
