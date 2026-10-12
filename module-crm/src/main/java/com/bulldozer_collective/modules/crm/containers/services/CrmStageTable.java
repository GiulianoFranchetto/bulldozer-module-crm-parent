package com.bulldozer_collective.modules.crm.containers.services;

import com.bulldozer_collective.modules.crm.api.CrmDealStage;
import com.bulldozer_collective.modules.crm.api.CrmLeadStage;
import com.bulldozer_collective.modules.crm.containers.data.CrmContainerEntity;
import com.bulldozer_collective.modules.crm.containers.data.CrmContainerStageEntity;
import com.bulldozer_collective.modules.crm.gateway.CrmStageWrite;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The stages of one container, loaded once per request: which value of the stage source property means which
 * Bulldozer stage, and which value to write for a stage.
 *
 * @param <S> the Bulldozer stage enum of the container's type ({@link CrmDealStage} or {@link CrmLeadStage}).
 */
public final class CrmStageTable<S extends Enum<S>> {

    private final String propertyName;
    private final String containerRemoteId;
    private final Map<String, CrmContainerStageEntity> byValue;
    private final Function<CrmContainerStageEntity, @Nullable S> stageOf;

    private CrmStageTable(@NonNull CrmContainerEntity container,
                          @NonNull Collection<CrmContainerStageEntity> stages,
                          @NonNull Function<CrmContainerStageEntity, @Nullable S> stageOf) {
        this.propertyName = container.getStagePropertyName();
        this.containerRemoteId = container.getRemoteId();
        this.byValue = stages.stream().collect(Collectors.toMap(
                CrmContainerStageEntity::getRemoteId, Function.identity(), (first, second) -> first));
        this.stageOf = stageOf;
    }

    public static @NonNull CrmStageTable<CrmDealStage> deals(@NonNull CrmContainerEntity container,
                                                             @NonNull Collection<CrmContainerStageEntity> stages) {
        return new CrmStageTable<>(container, stages, CrmContainerStageEntity::getDealStage);
    }

    public static @NonNull CrmStageTable<CrmLeadStage> leads(@NonNull CrmContainerEntity container,
                                                             @NonNull Collection<CrmContainerStageEntity> stages) {
        return new CrmStageTable<>(container, stages, CrmContainerStageEntity::getLeadStage);
    }

    /** The property to read on every record of the container. */
    public @NonNull String propertyName() {
        return this.propertyName;
    }

    public @NonNull String containerRemoteId() {
        return this.containerRemoteId;
    }

    /** The raw stage value among a record's properties. */
    public @Nullable String value(@NonNull Map<String, String> properties) {
        return properties.get(this.propertyName);
    }

    /** The Bulldozer stage a value is mapped to; {@code null} for an unmapped or unknown value. */
    public @Nullable S stage(@Nullable String value) {
        var stage = value == null ? null : this.byValue.get(value);
        return stage == null ? null : this.stageOf.apply(stage);
    }

    /**
     * The property and value that put a record in {@code stage}: the only value mapped to it, or, when there are
     * several, the one marked {@code writeDefault}.
     *
     * @throws IllegalArgumentException when no value maps to the stage, or several do and none is the write
     *                                  default.
     */
    public @NonNull CrmStageWrite write(@NonNull S stage) {
        var candidates = this.byValue.values().stream()
                .filter(candidate -> this.stageOf.apply(candidate) == stage)
                .toList();
        var chosen = choose(candidates);
        if (chosen == null) {
            throw new IllegalArgumentException(candidates.isEmpty()
                    ? "stage: %s is not mapped to any stage of this container".formatted(stage)
                    : "stage: %s is mapped to several stages of this container (%s) and none is the write default"
                            .formatted(stage, candidates.stream().map(CrmContainerStageEntity::getRemoteId)
                                    .sorted().collect(Collectors.joining(", "))));
        }
        return new CrmStageWrite(this.propertyName, chosen.getRemoteId());
    }

    private static @Nullable CrmContainerStageEntity choose(@NonNull List<CrmContainerStageEntity> candidates) {
        if (candidates.size() == 1) {
            return candidates.get(0);
        }
        return candidates.stream().filter(CrmContainerStageEntity::isWriteDefault).findFirst().orElse(null);
    }
}
