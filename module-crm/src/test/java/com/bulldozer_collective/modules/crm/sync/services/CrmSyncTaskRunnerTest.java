package com.bulldozer_collective.modules.crm.sync.services;

import com.bulldozer_collective.jobs.lib.JobNonRecoverableException;
import com.bulldozer_collective.jobs.lib.JobTask;
import com.bulldozer_collective.jobs.lib.JobTaskState;
import com.bulldozer_collective.logging.BulldozerLogger;
import com.bulldozer_collective.modules.crm.api.CrmSyncStatus;
import com.bulldozer_collective.shared.IntegrationPlatform;
import com.bulldozer_collective.shared.OAuthToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The task chain is the module's control flow: the order of the four steps is load-bearing (deals need their
 * pipeline stages, associations need the objects, linking needs the associations) and it is expressed only by
 * each task spawning the next. These tests pin that ordering and the criticality that goes with it.
 */
class CrmSyncTaskRunnerTest {

    private static final UUID CUSTOMER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PROJECT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID RUN_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final CrmSyncRunRef RUN_REF = new CrmSyncRunRef(CUSTOMER_ID, PROJECT_ID, RUN_ID);

    /** A record, so it cannot be mocked — and there is nothing to mock about it. */
    private static final OAuthToken TOKEN = OAuthToken.builder()
            .token("hubspot-access-token")
            .expiresAt(Instant.EPOCH.plusSeconds(1_800))
            .platform(IntegrationPlatform.INTEGRATION_PLATFORM_HUBSPOT)
            .appId("app")
            .build();

    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private CrmSyncPlatformResolver platformResolver;
    private PlatformTokenFetcher tokenFetcher;
    private CrmSyncValidator validator;
    private CrmSyncRunUpdater runUpdater;
    private CrmCanonicalLinker linker;
    private CrmPlatformSyncService adapter;
    private CrmSyncTaskRunner runner;

    @BeforeEach
    void setUp() throws Exception {
        this.platformResolver = mock(CrmSyncPlatformResolver.class);
        this.tokenFetcher = mock(PlatformTokenFetcher.class);
        this.validator = mock(CrmSyncValidator.class);
        this.runUpdater = mock(CrmSyncRunUpdater.class);
        this.linker = mock(CrmCanonicalLinker.class);
        this.adapter = mock(CrmPlatformSyncService.class);

        when(this.platformResolver.resolve(any())).thenReturn(this.adapter);
        when(this.tokenFetcher.fetchToken(any(), any(), any())).thenReturn(TOKEN);
        when(this.adapter.syncObjects(any(), any(), any())).thenReturn(CrmSyncResult.builder().build());

        this.runner = new CrmSyncTaskRunner(this.jsonMapper, this.platformResolver, this.tokenFetcher,
                this.validator, this.runUpdater, this.linker, mock(BulldozerLogger.class));
    }

    @Test
    @DisplayName("pipelines discovery hands over to the objects step, as a critical task")
    void pipelinesSpawnsObjects() throws Throwable {
        var result = this.runner.run(task(CrmSyncTaskRunner.SYNC_PIPELINES));

        verify(this.validator).validate(CUSTOMER_ID, PROJECT_ID, IntegrationPlatform.INTEGRATION_PLATFORM_HUBSPOT);
        assertThat(result.newTasks()).singleElement()
                .satisfies(next -> {
                    assertThat(next.type()).isEqualTo(CrmSyncTaskRunner.SYNC_OBJECTS);
                    assertThat(next.critical()).isTrue();
                });
    }

    @Test
    @DisplayName("objects hand over to associations, and the counts land on the run")
    void objectsSpawnAssociations() throws Throwable {
        var result = this.runner.run(task(CrmSyncTaskRunner.SYNC_OBJECTS));

        verify(this.runUpdater).recordCounts(eq(RUN_REF), any());
        assertThat(result.newTasks()).singleElement()
                .satisfies(next -> assertThat(next.type()).isEqualTo(CrmSyncTaskRunner.SYNC_ASSOCIATIONS));
    }

    @Test
    @DisplayName("linking is spawned non-critical: mirrored data stays valid if the contacts module is down")
    void associationsSpawnNonCriticalLinking() throws Throwable {
        var result = this.runner.run(task(CrmSyncTaskRunner.SYNC_ASSOCIATIONS));

        assertThat(result.newTasks()).singleElement()
                .satisfies(next -> {
                    assertThat(next.type()).isEqualTo(CrmSyncTaskRunner.LINK_CANONICAL);
                    assertThat(next.critical()).isFalse();
                });
    }

    @Test
    @DisplayName("linking reschedules itself while it is making progress")
    void linkingReschedulesWhileProgressing() throws Throwable {
        when(this.linker.linkBatch(any(), any(), any())).thenReturn(new CrmLinkProgress(200, true));

        var result = this.runner.run(task(CrmSyncTaskRunner.LINK_CANONICAL));

        assertThat(result.mustReschedule()).isTrue();
        assertThat(result.newTasks()).isNull();
    }

    @Test
    @DisplayName("linking stops when rows remain but none could be linked — otherwise it loops for ever")
    void linkingStopsWithoutProgress() throws Throwable {
        when(this.linker.linkBatch(any(), any(), any())).thenReturn(new CrmLinkProgress(0, true));

        var result = this.runner.run(task(CrmSyncTaskRunner.LINK_CANONICAL));

        assertThat(result.mustReschedule()).isFalse();
    }

    @Test
    @DisplayName("a failing task records why on the run before the job listener writes the terminal status")
    void failureIsRecordedOnTheRun() throws Exception {
        when(this.tokenFetcher.fetchToken(any(), any(), any()))
                .thenThrow(new NoTokenException(CUSTOMER_ID, PROJECT_ID,
                        IntegrationPlatform.INTEGRATION_PLATFORM_HUBSPOT));

        assertThatThrownBy(() -> this.runner.run(task(CrmSyncTaskRunner.SYNC_OBJECTS)))
                .isInstanceOf(NoTokenException.class);

        verify(this.runUpdater).finish(eq(RUN_REF), eq(CrmSyncStatus.CRM_SYNC_STATUS_FAILED),
                Mockito.contains("SYNC_OBJECTS"));
    }

    @Test
    @DisplayName("an unknown task type fails the job instead of being silently skipped")
    void unknownTypeIsNonRecoverable() {
        assertThatThrownBy(() -> this.runner.run(task("SYNC_SOMETHING_ELSE")))
                .isInstanceOf(JobNonRecoverableException.class);
    }

    @Test
    @DisplayName("a task with no payload cannot be run")
    void missingPayloadIsNonRecoverable() {
        var task = new JobTask(UUID.randomUUID(), Instant.EPOCH, Instant.EPOCH, UUID.randomUUID(),
                JobTaskState.PENDING, CrmSyncTaskRunner.SYNC_OBJECTS, null, true);

        assertThatThrownBy(() -> this.runner.run(task)).isInstanceOf(JobNonRecoverableException.class);
    }

    private JobTask task(String type) {
        var payload = this.jsonMapper.writeValueAsString(new CrmSyncPayload(
                CUSTOMER_ID, PROJECT_ID, IntegrationPlatform.INTEGRATION_PLATFORM_HUBSPOT, RUN_ID));
        return new JobTask(UUID.randomUUID(), Instant.EPOCH, Instant.EPOCH, UUID.randomUUID(),
                JobTaskState.PENDING, type, payload, true);
    }
}
