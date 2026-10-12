package com.bulldozer_collective.modules.crm.containers.services;

import com.bulldozer_collective.modules.crm.api.CrmContainerType;
import com.bulldozer_collective.modules.crm.connections.data.CrmConnectionEntity;
import com.bulldozer_collective.modules.crm.containers.data.CrmContainerEntity;
import com.bulldozer_collective.modules.crm.containers.data.CrmContainerRepository;
import com.bulldozer_collective.modules.crm.containers.data.CrmContainerStageEntity;
import com.bulldozer_collective.modules.crm.containers.data.CrmContainerStageRepository;
import com.bulldozer_collective.modules.crm.gateway.CrmStageOption;
import com.bulldozer_collective.logging.BulldozerLogger;
import com.bulldozer_collective.shared.exceptions.ConflictException;
import com.bulldozer_collective.shared.exceptions.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.bulldozer_collective.modules.crm.configurations.CrmLogContext.op;

/**
 * The containers' writes, each in one transaction. Everything read from the CRM (the pipeline, the stage values)
 * is read by the callers beforehand, outside any transaction.
 */
@Service
@RequiredArgsConstructor
public class CrmContainerPersister {

    private final CrmContainerRepository containerRepository;
    private final CrmContainerStageRepository stageRepository;
    private final BulldozerLogger logger;

    /** A container to import, with everything already read from the CRM. */
    public record NewContainer(@NonNull CrmContainerType type,
                               @NonNull String remoteId,
                               @NonNull String name,
                               @NonNull String stagePropertyName,
                               @NonNull List<CrmStageOption> options) {
    }

    /** @throws ConflictException when a container is already imported on that connection; nothing is imported. */
    @Transactional
    public @NonNull List<CrmContainerEntity> create(@NonNull CrmConnectionEntity connection,
                                                    @NonNull List<NewContainer> containers) {
        var duplicates = containers.stream()
                .filter(container -> this.containerRepository
                        .existsByCustomerIdAndProjectIdAndConnection_IdAndTypeAndRemoteId(connection.getCustomerId(),
                                connection.getProjectId(), connection.getId(), container.type(), container.remoteId()))
                .map(NewContainer::remoteId)
                .toList();
        if (!duplicates.isEmpty()) {
            throw new ConflictException("Already imported: " + String.join(", ", duplicates));
        }

        var saved = new ArrayList<CrmContainerEntity>();
        var stages = new ArrayList<CrmContainerStageEntity>();
        for (var container : containers) {
            var entity = new CrmContainerEntity();
            entity.setCustomerId(connection.getCustomerId());
            entity.setProjectId(connection.getProjectId());
            entity.setConnection(connection);
            entity.setType(container.type());
            entity.setRemoteId(container.remoteId());
            entity.setName(container.name());
            entity.setStagePropertyName(container.stagePropertyName());
            var persisted = this.containerRepository.save(entity);
            saved.add(persisted);
            container.options().forEach(option -> stages.add(newStage(persisted, option)));
        }
        this.stageRepository.saveAll(stages);

        this.logger.info(op("CRM-003", connection.getCustomerId(), connection.getProjectId())
                + "Imported %d containers on connection %s", saved.size(), connection.getId());
        return saved;
    }

    /**
     * Replaces the stage source: every stage, and its mapping, is replaced by the values of the new property.
     */
    @Transactional
    public @NonNull CrmContainerEntity replaceStages(@NonNull CrmContainerEntity container,
                                                     @NonNull String stagePropertyName,
                                                     @NonNull List<CrmStageOption> options) {
        this.stageRepository.deleteByCustomerIdAndProjectIdAndContainer_Id(
                container.getCustomerId(), container.getProjectId(), container.getId());
        // The deletes must reach the database before inserts that may reuse the same values.
        this.stageRepository.flush();
        container.setStagePropertyName(stagePropertyName);
        var saved = this.containerRepository.save(container);
        this.stageRepository.saveAll(options.stream().map(option -> newStage(saved, option)).toList());

        this.logger.info(op("CRM-003", container.getCustomerId(), container.getProjectId())
                + "Stage source of container %s is now %s", container.getId(), stagePropertyName);
        return saved;
    }

    /**
     * Aligns the container on the CRM: new name, new values added unmapped, renamed and reordered values updated
     * with their mapping kept, vanished values deleted with their mapping.
     */
    @Transactional
    public @NonNull CrmContainerEntity refresh(@NonNull CrmContainerEntity container,
                                               @NonNull String name,
                                               @NonNull List<CrmStageOption> options) {
        var existing = new HashMap<>(this.stageRepository
                .findAllByCustomerIdAndProjectIdAndContainer_IdOrderByDisplayOrderAsc(
                        container.getCustomerId(), container.getProjectId(), container.getId())
                .stream()
                .collect(Collectors.toMap(CrmContainerStageEntity::getRemoteId, Function.identity())));

        var kept = new ArrayList<CrmContainerStageEntity>();
        for (var option : options) {
            var stage = existing.remove(option.value());
            if (stage == null) {
                stage = newStage(container, option);
            } else {
                stage.setName(option.label());
                stage.setDisplayOrder(option.displayOrder());
            }
            kept.add(stage);
        }
        this.stageRepository.deleteAll(existing.values());
        this.stageRepository.saveAll(kept);

        container.setName(name);
        var saved = this.containerRepository.save(container);
        this.logger.info(op("CRM-003", container.getCustomerId(), container.getProjectId())
                + "Refreshed container %s: %d stages, %d removed", container.getId(), kept.size(), existing.size());
        return saved;
    }

    /**
     * Replaces the mapping of every listed stage. A write default given here takes the mark off every other stage
     * of the container mapped to the same Bulldozer stage; an unmapped stage is never a write default.
     *
     * @return every stage of the container, after the update.
     * @throws NotFoundException when a stage is not in the container; nothing is changed.
     */
    @Transactional
    public @NonNull List<CrmContainerStageEntity> map(@NonNull UUID customerId,
                                                      @NonNull UUID projectId,
                                                      @NonNull UUID containerId,
                                                      @NonNull List<MapCrmContainerStageBuilder> mappings) {
        var stages = this.stageRepository.findAllByCustomerIdAndProjectIdAndContainer_IdOrderByDisplayOrderAsc(
                customerId, projectId, containerId);
        var byId = stages.stream().collect(Collectors.toMap(CrmContainerStageEntity::getId, Function.identity()));

        for (var mapping : mappings) {
            var stage = byId.get(mapping.stageId());
            if (stage == null) {
                throw new NotFoundException("No stage " + mapping.stageId() + " in container " + containerId);
            }
            stage.setDealStage(mapping.dealStage());
            stage.setLeadStage(mapping.leadStage());
            stage.setWriteDefault(mapping.mapped() && mapping.writeDefault());
        }

        for (var mapping : mappings) {
            var chosen = byId.get(mapping.stageId());
            if (!chosen.isWriteDefault()) {
                continue;
            }
            stages.stream()
                    .filter(other -> other != chosen
                            && other.getDealStage() == chosen.getDealStage()
                            && other.getLeadStage() == chosen.getLeadStage())
                    .forEach(other -> other.setWriteDefault(false));
        }

        var saved = this.stageRepository.saveAll(stages);
        this.logger.info(op("CRM-003", customerId, projectId) + "Mapped %d stages of container %s",
                mappings.size(), containerId);
        return saved;
    }

    private static CrmContainerStageEntity newStage(@NonNull CrmContainerEntity container,
                                                    @NonNull CrmStageOption option) {
        var stage = new CrmContainerStageEntity();
        stage.setCustomerId(container.getCustomerId());
        stage.setProjectId(container.getProjectId());
        stage.setContainer(container);
        stage.setRemoteId(option.value());
        stage.setName(option.label());
        stage.setDisplayOrder(option.displayOrder());
        return stage;
    }
}
