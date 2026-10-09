package com.bulldozer_collective.modules.crm.contacts.controllers;

import com.bulldozer_collective.modules.crm.api.UpdateCrmContactRequest;
import com.bulldozer_collective.modules.crm.api.UpdateCrmContactResponse;
import com.bulldozer_collective.modules.crm.contacts.security.UpdateCrmContactAccessChecker;
import com.bulldozer_collective.modules.crm.contacts.services.CrmContactUpdater;
import com.bulldozer_collective.modules.crm.contacts.services.SaveCrmContactBuilder;
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
@RequestMapping("/crm/{connectionId}/contacts")
public class UpdateCrmContactController {

    private final CrmContactUpdater updater;
    private final UpdateCrmContactAccessChecker accessChecker;

    @PatchMapping("/{id}")
    public UpdateCrmContactResponse update(@JwtRequestContext RequestContext context,
                                           @PathVariable @NonNull UUID connectionId,
                                           @Valid @RequestBody @NonNull UpdateCrmContactRequest request,
                                           @PathVariable @NonNull String id) throws NoTokenException {
        this.accessChecker.checkAccess(context);
        var contact = this.updater.update(
                Objects.requireNonNull(context.targetCustomerId()),
                Objects.requireNonNull(context.projectId()),
                connectionId,
                id,
                SaveCrmContactBuilder.builder()
                        .firstName(request.firstName())
                        .lastName(request.lastName())
                        .email(request.email())
                        .phone(request.phone())
                        .jobTitle(request.jobTitle())
                        .ownerId(request.ownerId())
                        .build());
        return new UpdateCrmContactResponse(contact);
    }
}
