package com.bulldozer_collective.modules.crm.forms.controllers;

import com.bulldozer_collective.modules.crm.api.UpdateCrmFormRequest;
import com.bulldozer_collective.modules.crm.api.UpdateCrmFormResponse;
import com.bulldozer_collective.modules.crm.forms.security.UpdateCrmFormAccessChecker;
import com.bulldozer_collective.modules.crm.forms.services.CrmFormUpdater;
import com.bulldozer_collective.modules.crm.forms.services.SaveCrmFormBuilder;
import com.bulldozer_collective.modules.crm.gateway.NoTokenException;
import com.bulldozer_collective.security.annotations.JwtRequestContext;
import com.bulldozer_collective.security.resolvers.RequestContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/crm/{connectionId}/forms")
public class UpdateCrmFormController {

    private final CrmFormUpdater updater;
    private final UpdateCrmFormAccessChecker accessChecker;

    @PatchMapping("/{id}")
    public UpdateCrmFormResponse update(@JwtRequestContext RequestContext context,
                                        @PathVariable @NonNull UUID connectionId,
                                        @PathVariable @NonNull String id,
                                        @Valid @RequestBody @NonNull UpdateCrmFormRequest request) throws NoTokenException {
        this.accessChecker.checkAccess(context);
        var form = this.updater.update(
                Objects.requireNonNull(context.targetCustomerId()),
                Objects.requireNonNull(context.projectId()),
                connectionId,
                id,
                SaveCrmFormBuilder.builder()
                        .name(request.name())
                        .fields(request.fields() == null ? null : CrmFormFieldMapper.fields(request.fields()))
                        .submitButtonText(request.submitButtonText())
                        .thankYouMessage(request.thankYouMessage())
                        .redirectUrl(request.redirectUrl())
                        .language(request.language())
                        .legalConsent(request.legalConsent())
                        .build());
        return new UpdateCrmFormResponse(form);
    }
}
