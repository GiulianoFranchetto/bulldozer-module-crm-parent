package com.bulldozer_collective.modules.crm.stages.services;

import com.bulldozer_collective.modules.crm.gateway.CrmGatewayResolver;
import com.bulldozer_collective.modules.crm.gateway.CrmSessionOpener;
import com.bulldozer_collective.modules.crm.gateway.NoTokenException;
import com.bulldozer_collective.modules.crm.stages.data.CrmStageMappingEntity;
import com.bulldozer_collective.modules.crm.stages.data.CrmStageSourceRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CrmStageMappingFactory {

    private final CrmSessionOpener sessionOpener;
    private final CrmGatewayResolver gatewayResolver;
    private final CrmStageSourceRepository sourceRepository;
    private final CrmStagePersister persister;

    /**
     * Maps a value of the pipeline's stage source to a stage. The value is checked against the values the CRM
     * accepts for that property in that pipeline, unless the property is free-form.
     *
     * @throws IllegalArgumentException when the pipeline has no stage source, or the value is not one the CRM
     *                                  accepts.
     */
    public @NonNull CrmStageMappingEntity create(@NonNull UUID customerId,
                                                 @NonNull UUID projectId,
                                                 @NonNull UUID connectionId,
                                                 @NonNull String pipelineId,
                                                 @NonNull CreateCrmStageMappingBuilder mapping) throws NoTokenException {
        var session = this.sessionOpener.open(customerId, projectId, connectionId);
        var source = this.sourceRepository
                .findByCustomerIdAndProjectIdAndConnectionIdAndPipelineId(customerId, projectId, connectionId, pipelineId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Pipeline '%s' has no stage source: set one before mapping its values".formatted(pipelineId)));

        var accepted = this.gatewayResolver.resolve(session)
                .listStageValues(session, pipelineId, source.getPropertyName());
        if (!accepted.isEmpty() && !accepted.contains(mapping.value())) {
            throw new IllegalArgumentException("value: '%s' is not a value of '%s' in pipeline '%s'; accepted values: %s"
                    .formatted(mapping.value(), source.getPropertyName(), pipelineId, String.join(", ", accepted)));
        }

        return this.persister.saveMapping(customerId, projectId, connectionId, pipelineId, source.getPropertyName(),
                mapping);
    }
}
