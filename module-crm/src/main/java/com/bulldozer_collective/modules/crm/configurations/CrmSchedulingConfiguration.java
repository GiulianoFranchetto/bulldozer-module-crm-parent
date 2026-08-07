package com.bulldozer_collective.modules.crm.configurations;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Gives the module's {@code @Scheduled} methods their own pool.
 *
 * <p>Spring Boot's default scheduler is <b>single-threaded</b>: without this bean the job poller, the
 * twice-daily sync cron and the stale-run sweeper queue behind one another, so a sync that takes minutes delays
 * the cron and the sweeper by as long. Being the only {@link TaskScheduler} in the context, this bean is picked
 * up for every {@code @Scheduled} in the module — the same route {@code module-seo} takes.
 */
@Configuration
public class CrmSchedulingConfiguration {

    @Bean
    public TaskScheduler taskScheduler() {
        var scheduler = new ThreadPoolTaskScheduler();
        // One thread per scheduled method, plus room for a long sync to overlap the next tick.
        scheduler.setPoolSize(4);
        scheduler.setThreadNamePrefix("CRM-");
        // A sync in flight must be allowed to finish its current transaction on shutdown.
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        return scheduler;
    }
}
