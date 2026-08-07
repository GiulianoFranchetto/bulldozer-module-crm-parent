package com.bulldozer_collective.modules.crm.sync.services.hubspot;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * HubSpot client tunables.
 *
 * <p>An OAuth app is allowed on the order of 110 requests per 10 seconds per installed account, and HubSpot
 * answers {@code 429} with a {@code Retry-After} header when that is exceeded. The retry path keys off that
 * header rather than off a hard-coded quota, so a change on HubSpot's side does not silently break us; the
 * throttle below is the belt to that braces.
 *
 * @param minRequestInterval spacing between two calls, across all threads. 100ms ≈ 100 requests / 10 s.
 * @param maxRetries         attempts after the first on a 429 or 5xx, before the task is failed.
 * @param initialBackoff     first wait when the response carries no {@code Retry-After}; doubles per attempt.
 * @param maxBackoff         ceiling for that doubling.
 * @param maxPagesPerObject  guard against a paging cursor that never terminates.
 */
@ConfigurationProperties(prefix = "bulldozer.crm.hubspot")
public record HubSpotProperties(Duration minRequestInterval,
                                Integer maxRetries,
                                Duration initialBackoff,
                                Duration maxBackoff,
                                Integer maxPagesPerObject) {

    public HubSpotProperties {
        minRequestInterval = minRequestInterval == null ? Duration.ofMillis(100) : minRequestInterval;
        maxRetries = maxRetries == null ? 4 : maxRetries;
        initialBackoff = initialBackoff == null ? Duration.ofSeconds(1) : initialBackoff;
        maxBackoff = maxBackoff == null ? Duration.ofSeconds(30) : maxBackoff;
        maxPagesPerObject = maxPagesPerObject == null ? 10_000 : maxPagesPerObject;
    }
}
