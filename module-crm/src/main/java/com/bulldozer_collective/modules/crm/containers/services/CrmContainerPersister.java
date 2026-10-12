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

    /** @throws ConflictException when the pipeline is already imported on that connection. */
    @Transactional
    public @NonNull CrmContainerEntity create(@NonNull CrmConnectionEntity connection,
                                              @NonNull CrmContainerType type,
                                              @NonNull String remoteId,
                                              @NonNull String name,
                                              @NonNull String stagePropertyName,
                                              @NonNull List<CrmStageOption> options) {
        if (this.containerRepository.existsByCustomerIdAndProjectIdAndConnection_IdAndTypeAndRemoteId(
                connection.getCustomerId(), connection.getProjectId(), connection.getId(), type, remoteId)) {
            throw new ConflictException("Pipeline '%s' is already imported".formatted(remoteId));
        }

        var container = new CrmContainerEntity();
        container.setCustomerId(connection.getCustomerId());
        container.setProjectId(connection.getProjectId());
        container.setConnection(connection);
        container.setType(type);
        container.setRemoteId(remoteId);
        container.setName(name);
        container.setStagePropertyName(stagePropertyName);
        var saved = this.containerRepository.save(container);
        this.stageRepository.saveAll(options.stream().map(option -> newStage(saved, option)).toList());

        this.logger.info(op("CRM-003", connection.getCustomerId(), connection.getProjectId())
                + "Imported %s pipeline %s as container %s", type, remoteId, saved.getId());
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
     * Sets the mapping of one stage. Marking it {@code writeDefault} takes the mark off every other stage mapped
     * to the same Bulldozer stage in the container.
     *
     * <p>{@code containerId} is passed explicitly: the stage was loaded outside this transaction, and its lazy
     * container cannot be read here.
     */
    @Transactional
    public @NonNull CrmContainerStageEntity map(@NonNull UUID containerId,
                                                @NonNull CrmContainerStageEntity stage,
                                                @NonNull MapCrmContainerStageBuilder mapping) {
        stage.setDealStage(mapping.dealStage());
        stage.setLeadStage(mapping.leadStage());
        var mapped = mapping.dealStage() != null || mapping.leadStage() != null;
        stage.setWriteDefault(mapped && mapping.writeDefault());

        if (stage.isWriteDefault()) {
            var others = this.stageRepository
                    .findAllByCustomerIdAndProjectIdAndContainer_IdOrderByDisplayOrderAsc(
                            stage.getCustomerId(), stage.getProjectId(), containerId)
                    .stream()
                    .filter(other -> !other.getId().equals(stage.getId())
                            && other.getDealStage() == stage.getDealStage()
                            && other.getLeadStage() == stage.getLeadStage()
                            && other.isWriteDefault())
                    .toList();
            others.forEach(other -> other.setWriteDefault(false));
            this.stageRepository.saveAll(others);
        }

        var saved = this.stageRepository.save(stage);
        this.logger.info(op("CRM-003", stage.getCustomerId(), stage.getProjectId())
                + "Stage %s of container %s mapped to %s", stage.getRemoteId(), containerId,
                mapping.dealStage() != null ? mapping.dealStage() : mapping.leadStage());
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
