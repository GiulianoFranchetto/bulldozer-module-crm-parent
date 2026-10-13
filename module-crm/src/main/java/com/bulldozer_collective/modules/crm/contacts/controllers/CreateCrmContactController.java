package com.bulldozer_collective.modules.crm.contacts.controllers;

import com.bulldozer_collective.modules.crm.api.CreateCrmContactRequest;
import com.bulldozer_collective.modules.crm.api.CreateCrmContactResponse;
import com.bulldozer_collective.modules.crm.contacts.security.CreateCrmContactAccessChecker;
import com.bulldozer_collective.modules.crm.contacts.services.CrmContactFactory;
import com.bulldozer_collective.modules.crm.contacts.services.SaveCrmContactBuilder;
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
@RequestMapping("/crm/{connectionId}/contacts")
public class CreateCrmContactController {

    private final CrmContactFactory factory;
    private final CreateCrmContactAccessChecker accessChecker;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateCrmContactResponse create(@JwtRequestContext RequestContext context,
                                           @PathVariable @NonNull UUID connectionId,
                                           @Valid @RequestBody @NonNull CreateCrmContactRequest request) throws NoTokenException {
        this.accessChecker.checkAccess(context);
        var contact = this.factory.create(
                Objects.requireNonNull(context.targetCustomerId()),
                Objects.requireNonNull(context.projectId()),
                connectionId,
                SaveCrmContactBuilder.builder()
                        .firstName(request.firstName())
                        .lastName(request.lastName())
                        .email(request.email())
                        .phone(request.phone())
                        .jobTitle(request.jobTitle())
                        .legalBasis(request.legalBasis())
                        .build());
        return new CreateCrmContactResponse(contact);
    }
}
