package com.bulldozer_collective.modules.crm.configurations;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class WebClientConfiguration {

    /** Long enough for a slow HubSpot page, short enough that a wedged socket cannot outlive a sync. */
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(60);

    /**
     * The sync's only outbound HTTP client.
     *
     * <p>The timeouts are the point: a bare {@code new RestTemplate()} has <b>none</b>, so a HubSpot socket that
     * never answers blocks its caller for good — and since the sync runs on the module's scheduler, that meant
     * the job poller, the twice-daily cron and the stale-run sweeper all wedged behind it, silently. Buffered so
     * an error body can be read twice (status check, then message).
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate(timeoutRequestFactory());
    }

    @Bean
    public JsonMapper jsonMapper() {
        return JsonMapper.builder()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .build();
    }

    private static ClientHttpRequestFactory timeoutRequestFactory() {
        var httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NEVER)
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
        var factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(READ_TIMEOUT);
        return new BufferingClientHttpRequestFactory(factory);
    }
}
