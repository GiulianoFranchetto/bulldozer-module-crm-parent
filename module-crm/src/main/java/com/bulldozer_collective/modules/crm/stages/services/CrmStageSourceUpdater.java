package com.bulldozer_collective.modules.crm.stages.services;

import com.bulldozer_collective.modules.crm.api.CrmDealProperty;
import com.bulldozer_collective.modules.crm.api.CrmPipeline;
import com.bulldozer_collective.modules.crm.gateway.CrmGatewayResolver;
import com.bulldozer_collective.modules.crm.gateway.CrmSessionOpener;
import com.bulldozer_collective.modules.crm.gateway.NoTokenException;
import com.bulldozer_collective.modules.crm.stages.data.CrmStageSourceEntity;
import com.bulldozer_collective.shared.exceptions.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CrmStageSourceUpdater {

    private final CrmSessionOpener sessionOpener;
    private final CrmGatewayResolver gatewayResolver;
    private final CrmStagePersister persister;

    /**
     * Checks the pipeline and the property against the CRM, then sets the source.
     *
     * @throws NotFoundException        when the CRM has no such pipeline.
     * @throws IllegalArgumentException when the CRM has no such deal property.
     */
    public @NonNull CrmStageSourceEntity set(@NonNull UUID customerId,
                                             @NonNull UUID projectId,
                                             @NonNull UUID connectionId,
                                             @NonNull String pipelineId,
                                             @NonNull String propertyName) throws NoTokenException {
        var session = this.sessionOpener.open(customerId, projectId, connectionId);
        var gateway = this.gatewayResolver.resolve(session);

        if (gateway.listPipelines(session).stream().map(CrmPipeline::id).noneMatch(pipelineId::equals)) {
            throw new NotFoundException("The CRM has no deal pipeline " + pipelineId);
        }
        if (gateway.listDealProperties(session).stream().map(CrmDealProperty::name).noneMatch(propertyName::equals)) {
            throw new IllegalArgumentException(
                    "propertyName: '%s' is not a deal property of this CRM".formatted(propertyName));
        }

        return this.persister.saveSource(customerId, projectId, connectionId, pipelineId, propertyName);
    }
}
