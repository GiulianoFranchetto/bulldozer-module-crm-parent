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
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "crm_sync_runs")
public class CrmSyncRunEntity extends ProjectAwareEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IntegrationPlatform platform;

    /**
     * Id of the job-library job carrying out this run. Nullable: the run row is written first, and the job is
     * only created once the enqueue succeeds. The job tables are purged by the job cleaner, so a run older
     * than the retention window keeps its own status but can no longer be resolved to a job report.
     */
    @Nullable
    @Column
    private UUID jobId;

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
