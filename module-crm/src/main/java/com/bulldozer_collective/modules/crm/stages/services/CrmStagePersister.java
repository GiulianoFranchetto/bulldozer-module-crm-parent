package com.bulldozer_collective.modules.crm.stages.services;

import com.bulldozer_collective.modules.crm.stages.data.CrmStageMappingEntity;
import com.bulldozer_collective.modules.crm.stages.data.CrmStageMappingRepository;
import com.bulldozer_collective.modules.crm.stages.data.CrmStageSourceEntity;
import com.bulldozer_collective.modules.crm.stages.data.CrmStageSourceRepository;
import com.bulldozer_collective.logging.BulldozerLogger;
import com.bulldozer_collective.shared.exceptions.ConflictException;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static com.bulldozer_collective.modules.crm.configurations.CrmLogContext.op;

/**
 * The stage configuration's writes, each in one transaction. The CRM-side checks (the pipeline exists, the value
 * is one the property accepts) are done by the callers beforehand, outside any transaction.
 */
@Service
@RequiredArgsConstructor
public class CrmStagePersister {

    private final CrmStageSourceRepository sourceRepository;
    private final CrmStageMappingRepository mappingRepository;
    private final BulldozerLogger logger;

    /**
     * Sets the pipeline's stage source. Changing the property drops the pipeline's mappings: they were values of
     * the previous property and mean nothing for the new one.
     */
    @Transactional
    public @NonNull CrmStageSourceEntity saveSource(@NonNull UUID customerId,
                                                    @NonNull UUID projectId,
                                                    @NonNull UUID connectionId,
                                                    @NonNull String pipelineId,
                                                    @NonNull String propertyName) {
        var source = this.sourceRepository
                .findByCustomerIdAndProjectIdAndConnectionIdAndPipelineId(customerId, projectId, connectionId, pipelineId)
                .orElse(null);
        if (source != null && source.getPropertyName().equals(propertyName)) {
            return source;
        }

        if (source == null) {
            source = new CrmStageSourceEntity();
            source.setCustomerId(customerId);
            source.setProjectId(projectId);
            source.setConnectionId(connectionId);
            source.setPipelineId(pipelineId);
        } else {
            this.mappingRepository.deleteByCustomerIdAndProjectIdAndConnectionIdAndPipelineId(
                    customerId, projectId, connectionId, pipelineId);
        }
        source.setPropertyName(propertyName);
        var saved = this.sourceRepository.save(source);
        this.logger.info(op("CRM-003", customerId, projectId) + "Stage source of pipeline %s on connection %s is now %s",
                pipelineId, connectionId, propertyName);
        return saved;
    }

    /**
     * Adds a mapping. Marking it {@code writeDefault} takes the mark off any other value mapped to the same stage
     * in that pipeline.
     *
     * @param expectedProperty the stage source the value was checked against; if it changed meanwhile, the value
     *                         is meaningless and the write is refused.
     */
    @Transactional
    public @NonNull CrmStageMappingEntity saveMapping(@NonNull UUID customerId,
                                                      @NonNull UUID projectId,
                                                      @NonNull UUID connectionId,
                                                      @NonNull String pipelineId,
                                                      @NonNull String expectedProperty,
                                                      @NonNull CreateCrmStageMappingBuilder mapping) {
        var source = this.sourceRepository
                .findByCustomerIdAndProjectIdAndConnectionIdAndPipelineId(customerId, projectId, connectionId, pipelineId)
                .orElse(null);
        if (source == null || !source.getPropertyName().equals(expectedProperty)) {
            throw new ConflictException("The stage source of pipeline '%s' changed meanwhile".formatted(pipelineId));
        }
        if (this.mappingRepository.existsByCustomerIdAndProjectIdAndConnectionIdAndPipelineIdAndValue(
                customerId, projectId, connectionId, pipelineId, mapping.value())) {
            throw new ConflictException("value: '%s' is already mapped in pipeline '%s'"
                    .formatted(mapping.value(), pipelineId));
        }

        if (mapping.writeDefault()) {
            var others = this.mappingRepository.findAllByCustomerIdAndProjectIdAndConnectionIdAndPipelineIdAndStage(
                    customerId, projectId, connectionId, pipelineId, mapping.stage());
            others.forEach(other -> other.setWriteDefault(false));
            this.mappingRepository.saveAll(others);
        }

        var entity = new CrmStageMappingEntity();
        entity.setCustomerId(customerId);
        entity.setProjectId(projectId);
        entity.setConnectionId(connectionId);
        entity.setPipelineId(pipelineId);
        entity.setValue(mapping.value());
        entity.setStage(mapping.stage());
        entity.setWriteDefault(mapping.writeDefault());
        var saved = this.mappingRepository.save(entity);
        this.logger.info(op("CRM-003", customerId, projectId) + "Mapped '%s' to %s in pipeline %s on connection %s",
                mapping.value(), mapping.stage(), pipelineId, connectionId);
        return saved;
    }
}
