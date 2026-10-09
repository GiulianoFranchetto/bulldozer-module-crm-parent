package com.bulldozer_collective.modules.crm.stages.services;

import com.bulldozer_collective.modules.crm.api.CrmDeal;
import com.bulldozer_collective.modules.crm.api.CrmDealStage;
import com.bulldozer_collective.modules.crm.gateway.CrmDealRecord;
import com.bulldozer_collective.modules.crm.gateway.CrmStageWrite;
import com.bulldozer_collective.modules.crm.stages.data.CrmStageMappingEntity;
import com.bulldozer_collective.modules.crm.stages.data.CrmStageSourceEntity;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * The stage configuration of one connection, loaded once per request: for each pipeline, the property that holds
 * the stage and what each of its values means.
 */
public final class CrmStageTable {

    private final Map<String, String> propertyByPipeline;
    /** pipelineId → value → mapping */
    private final Map<String, Map<String, CrmStageMappingEntity>> mappingsByPipeline;

    public CrmStageTable(@NonNull Collection<CrmStageSourceEntity> sources,
                         @NonNull Collection<CrmStageMappingEntity> mappings) {
        this.propertyByPipeline = sources.stream()
                .collect(Collectors.toMap(CrmStageSourceEntity::getPipelineId, CrmStageSourceEntity::getPropertyName));
        this.mappingsByPipeline = new HashMap<>();
        mappings.forEach(mapping -> this.mappingsByPipeline
                .computeIfAbsent(mapping.getPipelineId(), pipelineId -> new HashMap<>())
                .put(mapping.getValue(), mapping));
    }

    /** Every property used as a stage source: the deals must be read with them. */
    public @NonNull Set<String> propertyNames() {
        return new TreeSet<>(this.propertyByPipeline.values());
    }

    /**
     * Fills {@code stageValue} and {@code stage}. Both stay {@code null} when the deal's pipeline has no stage
     * source; {@code stage} alone stays {@code null} when the value is not mapped.
     */
    public @NonNull CrmDeal resolve(@NonNull CrmDealRecord record) {
        var deal = record.deal();
        var property = deal.pipelineId() == null ? null : this.propertyByPipeline.get(deal.pipelineId());
        if (property == null) {
            return deal;
        }
        var value = record.properties().get(property);
        var mapping = value == null
                ? null
                : this.mappingsByPipeline.getOrDefault(deal.pipelineId(), Map.of()).get(value);
        return deal.toBuilder()
                .stageValue(value)
                .stage(mapping == null ? null : mapping.getStage())
                .build();
    }

    /**
     * The property and value that put a deal of {@code pipelineId} in {@code stage}: the only value mapped to that
     * stage, or, when there are several, the one marked {@code writeDefault}.
     *
     * @throws IllegalArgumentException when the pipeline has no stage source, the stage is not mapped, or it maps
     *                                  to several values and none is the write default.
     */
    public @NonNull CrmStageWrite write(@NonNull String pipelineId, @NonNull CrmDealStage stage) {
        var property = this.propertyByPipeline.get(pipelineId);
        if (property == null) {
            throw new IllegalArgumentException(
                    "Pipeline '%s' has no stage source: configure it before writing a stage".formatted(pipelineId));
        }

        var candidates = this.mappingsByPipeline.getOrDefault(pipelineId, Map.of()).values().stream()
                .filter(mapping -> mapping.getStage() == stage)
                .toList();
        var chosen = choose(candidates);
        if (chosen == null) {
            throw new IllegalArgumentException(candidates.isEmpty()
                    ? "stage: %s is not mapped to any value of '%s' in pipeline '%s'"
                            .formatted(stage, property, pipelineId)
                    : "stage: %s maps to several values of '%s' in pipeline '%s' (%s) and none is the write default"
                            .formatted(stage, property, pipelineId, values(candidates)));
        }
        return new CrmStageWrite(property, chosen.getValue());
    }

    private static @Nullable CrmStageMappingEntity choose(@NonNull List<CrmStageMappingEntity> candidates) {
        if (candidates.size() == 1) {
            return candidates.get(0);
        }
        return candidates.stream().filter(CrmStageMappingEntity::isWriteDefault).findFirst().orElse(null);
    }

    private static String values(@NonNull List<CrmStageMappingEntity> mappings) {
        return mappings.stream().map(CrmStageMappingEntity::getValue).sorted().collect(Collectors.joining(", "));
    }
}
