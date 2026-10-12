package com.bulldozer_collective.modules.crm.containers.services;

import com.bulldozer_collective.modules.crm.api.CrmContainerType;
import com.bulldozer_collective.modules.crm.containers.data.CrmContainerEntity;
import com.bulldozer_collective.modules.crm.gateway.CrmGateway;
import com.bulldozer_collective.modules.crm.gateway.CrmSession;
import com.bulldozer_collective.modules.crm.gateway.CrmStageOption;
import com.bulldozer_collective.modules.crm.gateway.CrmStageWrite;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.util.List;

/** Reads the values of a stage source property from the CRM, checks it can be one, and decides what a create writes. */
@Service
public class CrmStageSources {

    /**
     * @throws IllegalArgumentException when the property does not exist, or is free-form: a container's stages
     *                                  must be a finite list.
     */
    public @NonNull List<CrmStageOption> read(@NonNull CrmSession session,
                                              @NonNull CrmGateway gateway,
                                              @NonNull CrmContainerType type,
                                              @NonNull String containerRemoteId,
                                              @NonNull String propertyName) {
        var options = gateway.listStageOptions(session, type, containerRemoteId, propertyName);
        if (options.isEmpty()) {
            throw new IllegalArgumentException(("stagePropertyName: '%s' has no fixed list of values in this "
                    + "pipeline; choose an enumerated property").formatted(propertyName));
        }
        return options;
    }

    /**
     * The stage values a new deal or lead is created with: the one its mapping designates and, when the container
     * reads its stages from a custom property, the pipeline's own stage the CRM files every record under — which
     * only the caller can choose.
     *
     * @param mapped        the value {@code CrmStageTable#write} designates for the requested stage.
     * @param pipelineStage the pipeline's own stage the caller gave, if any.
     * @throws IllegalArgumentException when {@code pipelineStage} is missing for a custom stage source, given for the
     *                                  native one, or not a stage of the pipeline.
     */
    public @NonNull List<CrmStageWrite> creationWrites(@NonNull CrmSession session,
                                                       @NonNull CrmGateway gateway,
                                                       @NonNull CrmContainerEntity container,
                                                       @NonNull CrmStageWrite mapped,
                                                       @Nullable String pipelineStage) {
        var nativeProperty = gateway.nativeStageProperty(container.getType());
        if (nativeProperty.equals(container.getStagePropertyName())) {
            if (pipelineStage != null) {
                throw new IllegalArgumentException("pipelineStage: not allowed, this container's stages are the "
                        + "pipeline's own; give stage only");
            }
            return List.of(mapped);
        }

        if (pipelineStage == null) {
            throw new IllegalArgumentException(("pipelineStage: required, this container reads its stages from "
                    + "'%s'; give the pipeline stage the CRM files the record under (stages of GET …/containers)")
                    .formatted(container.getStagePropertyName()));
        }
        var known = gateway.listStageOptions(session, container.getType(), container.getRemoteId(), nativeProperty)
                .stream()
                .anyMatch(option -> option.value().equals(pipelineStage));
        if (!known) {
            throw new IllegalArgumentException(
                    "pipelineStage: '%s' is not a stage of this pipeline".formatted(pipelineStage));
        }
        return List.of(mapped, new CrmStageWrite(nativeProperty, pipelineStage));
    }
}
