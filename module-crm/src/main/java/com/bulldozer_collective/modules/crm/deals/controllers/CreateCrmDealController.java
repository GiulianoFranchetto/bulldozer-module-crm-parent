package com.bulldozer_collective.modules.crm.deals.controllers;

import com.bulldozer_collective.modules.crm.api.CreateCrmDealRequest;
import com.bulldozer_collective.modules.crm.api.CreateCrmDealResponse;
import com.bulldozer_collective.modules.crm.api.CrmContainerParam;
import com.bulldozer_collective.modules.crm.deals.security.CreateCrmDealAccessChecker;
import com.bulldozer_collective.modules.crm.deals.services.CrmDealFactory;
import com.bulldozer_collective.modules.crm.deals.services.SaveCrmDealBuilder;
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
@RequestMapping("/crm/{connectionId}/deals")
public class CreateCrmDealController {

    private final CrmDealFactory factory;
    private final CreateCrmDealAccessChecker accessChecker;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateCrmDealResponse create(@JwtRequestContext RequestContext context,
                                        @PathVariable @NonNull UUID connectionId,
                                        @Valid @RequestBody @NonNull CreateCrmDealRequest request,
                                        @Valid @NonNull CrmContainerParam param) throws NoTokenException {
        this.accessChecker.checkAccess(context);
        var deal = this.factory.create(
                Objects.requireNonNull(context.targetCustomerId()),
                Objects.requireNonNull(context.projectId()),
                connectionId,
                param.containerId(),
                SaveCrmDealBuilder.builder()
                        .name(request.name())
                        .stage(request.stage())
                        .amount(request.amount())
                        .currency(request.currency())
                        .closeDate(request.closeDate())
                        .pipelineStage(request.pipelineStage())
                        .build());
        return new CreateCrmDealResponse(deal);
    }
}
