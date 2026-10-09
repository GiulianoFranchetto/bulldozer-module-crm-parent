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

    /** Long enough for a slow HubSpot search, short enough that a wedged socket cannot hold a request thread. */
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(30);

    /**
     * The module's only outbound HTTP client, used by the CRM gateways.
     *
     * <p>The timeouts are the point: a bare {@code new RestTemplate()} has <b>none</b>, so a CRM socket that never
     * answers would hold the caller's request — and its servlet thread — for good. Buffered so an error body can
     * be read twice (status check, then message).
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
