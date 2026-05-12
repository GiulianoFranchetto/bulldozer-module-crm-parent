package com.bulldozer_collective.modules.crm.sync.data;

import com.bulldozer_collective.shared.IntegrationPlatform;
import com.bulldozer_collective.modules.crm.api.CrmSyncStatus;
import com.bulldozer_collective.shared.jpa.ProjectAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "crm_sync_runs")
public class CrmSyncRunEntity extends ProjectAwareEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IntegrationPlatform platform;

    @Column(nullable = false)
    private Instant startedAt;

    @Nullable
    @Column
    private Instant finishedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CrmSyncStatus status;

    @Column(nullable = false)
    private int contactsSynced;

    @Column(nullable = false)
    private int companiesSynced;

    @Column(nullable = false)
    private int dealsSynced;

    @Column(nullable = false)
    private int activitiesSynced;

    @Nullable
    @Column(columnDefinition = "TEXT")
    private String errorMessage;
}
