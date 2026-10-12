package com.bulldozer_collective.modules.crm.deals.controllers;

import com.bulldozer_collective.modules.crm.api.CrmContainerParam;
import com.bulldozer_collective.modules.crm.api.UpdateCrmDealRequest;
import com.bulldozer_collective.modules.crm.api.UpdateCrmDealResponse;
import com.bulldozer_collective.modules.crm.deals.security.UpdateCrmDealAccessChecker;
import com.bulldozer_collective.modules.crm.deals.services.CrmDealUpdater;
import com.bulldozer_collective.modules.crm.deals.services.SaveCrmDealBuilder;
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
@RequestMapping("/crm/{connectionId}/deals")
public class UpdateCrmDealController {

    private final CrmDealUpdater updater;
    private final UpdateCrmDealAccessChecker accessChecker;

    @PatchMapping("/{id}")
    public UpdateCrmDealResponse update(@JwtRequestContext RequestContext context,
                                        @PathVariable @NonNull UUID connectionId,
                                        @Valid @RequestBody @NonNull UpdateCrmDealRequest request,
                                        @PathVariable @NonNull String id,
                                        @Valid @NonNull CrmContainerParam param) throws NoTokenException {
        this.accessChecker.checkAccess(context);
        var deal = this.updater.update(
                Objects.requireNonNull(context.targetCustomerId()),
                Objects.requireNonNull(context.projectId()),
                connectionId,
                param.containerId(),
                id,
                SaveCrmDealBuilder.builder()
                        .name(request.name())
                        .stage(request.stage())
                        .amount(request.amount())
                        .currency(request.currency())
                        .closeDate(request.closeDate())
                        .build());
        return new UpdateCrmDealResponse(deal);
    }
}
