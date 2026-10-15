package com.bulldozer_collective.modules.crm.forms.controllers;

import com.bulldozer_collective.modules.crm.api.CreateCrmFormRequest;
import com.bulldozer_collective.modules.crm.api.CreateCrmFormResponse;
import com.bulldozer_collective.modules.crm.api.CrmFormLegalConsent;
import com.bulldozer_collective.modules.crm.api.CrmFormLegalConsentType;
import com.bulldozer_collective.modules.crm.forms.security.CreateCrmFormAccessChecker;
import com.bulldozer_collective.modules.crm.forms.services.CrmFormFactory;
import com.bulldozer_collective.modules.crm.forms.services.SaveCrmFormBuilder;
import com.bulldozer_collective.modules.crm.gateway.NoTokenException;
import com.bulldozer_collective.security.annotations.JwtRequestContext;
import com.bulldozer_collective.security.resolvers.RequestContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/crm/{connectionId}/forms")
public class CreateCrmFormController {

    private static final CrmFormLegalConsent NO_CONSENT =
            new CrmFormLegalConsent(CrmFormLegalConsentType.CRM_FORM_LEGAL_CONSENT_TYPE_NONE, null, null);

    private final CrmFormFactory factory;
    private final CreateCrmFormAccessChecker accessChecker;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateCrmFormResponse create(@JwtRequestContext RequestContext context,
                                        @PathVariable @NonNull UUID connectionId,
                                        @Valid @RequestBody @NonNull CreateCrmFormRequest request) throws NoTokenException {
        this.accessChecker.checkAccess(context);
        var form = this.factory.create(
                Objects.requireNonNull(context.targetCustomerId()),
                Objects.requireNonNull(context.projectId()),
                connectionId,
                SaveCrmFormBuilder.builder()
                        .name(request.name())
                        .fields(CrmFormFieldMapper.fields(request.fields()))
                        .submitButtonText(request.submitButtonText())
                        .thankYouMessage(request.thankYouMessage())
                        .redirectUrl(request.redirectUrl())
                        .language(request.language())
                        .legalConsent(request.legalConsent() == null ? NO_CONSENT : request.legalConsent())
                        .build());
        return new CreateCrmFormResponse(form);
    }
}
