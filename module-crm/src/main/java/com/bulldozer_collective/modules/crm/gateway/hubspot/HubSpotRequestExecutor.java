package com.bulldozer_collective.modules.crm.gateway.hubspot;

import com.bulldozer_collective.logging.BulldozerLogger;
import com.bulldozer_collective.modules.crm.gateway.CrmUpstreamException;
import com.bulldozer_collective.shared.exceptions.InternalException;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import static com.bulldozer_collective.modules.crm.configurations.CrmLogContext.op;

/**
 * Throttles and retries every HubSpot call.
 *
 * <p>Every call runs inside a caller's HTTP request, so a {@code 429} — routine on the search endpoints, capped
 * at five requests per second per account — or a transient {@code 5xx} would otherwise surface as a failed
 * request for something a short wait fixes.
 *
 * <p>Two mechanisms, in this order:
 * <ol>
 *   <li>a throttle per connection, shared across threads, so each connection's rate stays under its account's
 *       quota whatever the number of requests in flight — and one busy connection never slows another down. Two
 *       projects connected to the same HubSpot account get a throttle each for one quota: the {@code 429} retry
 *       below covers that rare case. A call whose turn is further away than {@code maxBackoff} is refused
 *       ({@code 502}) rather than left sleeping;</li>
 *   <li>a bounded retry that honours HubSpot's {@code Retry-After} header and otherwise backs off
 *       exponentially. Bounded on purpose, and with a short ceiling: the caller is waiting on the other end.</li>
 * </ol>
 *
 * <p>A {@code 4xx} other than {@code 429} is not retried: the request is wrong, and repeating it wastes quota.
 * Neither is a write that HubSpot rejected as a duplicate ({@code 409}): retrying it could only fail again.
 *
 * <p>A {@code 429} is retried for every call: HubSpot refused it, so it did nothing. A timeout or a {@code 5xx} is
 * retried only for an idempotent call — a create may have succeeded behind it, and replaying it would duplicate the
 * record in the customer's CRM, where the bridge keeps nothing to deduplicate against.
 */
@Component
@RequiredArgsConstructor
public class HubSpotRequestExecutor {

    private final HubSpotProperties properties;
    private final BulldozerLogger logger;

    /**
     * Per connection, the next epoch-milli at which a call is allowed; shared by every thread. One entry per connection
     * ever called, a handful of bytes each: there is at most one connection per project and platform.
     */
    private final Map<String, AtomicLong> nextAllowedAt = new ConcurrentHashMap<>();

    /**
     * @param account    the connection the call is made for, keying its throttle.
     * @param idempotent whether the call may be replayed after a timeout or a {@code 5xx}.
     * @throws CrmUpstreamException when the connection's throttle is saturated.
     */
    public <T> T execute(@NonNull String account,
                         @NonNull String description,
                         boolean idempotent,
                         @NonNull Supplier<T> call) {
        var attempts = this.properties.maxRetries() + 1;
        RuntimeException last = null;

        for (var attempt = 1; attempt <= attempts; attempt++) {
            this.throttle(account);
            try {
                return call.get();
            } catch (HttpClientErrorException e) {
                if (e.getStatusCode().value() != HttpStatus.TOO_MANY_REQUESTS.value()) {
                    // A wrong request will stay wrong: fail now rather than burn quota repeating it.
                    throw e;
                }
                last = e;
                this.pause(description, attempt, retryAfter(e.getResponseHeaders()));
            } catch (HttpServerErrorException | ResourceAccessException e) {
                // 5xx and read timeouts are the transient cases worth another attempt — unless the call may have
                // taken effect anyway.
                if (!idempotent) {
                    throw e;
                }
                last = e;
                this.pause(description, attempt, null);
            }
        }

        this.logger.error(op("CRM-002") + "HubSpot call '%s' still failing after %d attempts",
                description, attempts);
        throw last != null ? last : new InternalException("HubSpot call failed: " + description);
    }

    private void pause(@NonNull String description, int attempt, @Nullable Duration retryAfter) {
        var backoff = retryAfter != null ? retryAfter : this.exponentialBackoff(attempt);
        if (backoff.compareTo(this.properties.maxBackoff()) > 0) {
            backoff = this.properties.maxBackoff();
        }

        this.logger.warn(op("CRM-002") + "HubSpot call '%s' failed (attempt %d), retrying in %s",
                description, attempt, backoff);
        sleep(backoff.toMillis());
    }

    private Duration exponentialBackoff(int attempt) {
        return this.properties.initialBackoff().multipliedBy(1L << (attempt - 1));
    }

    /**
     * HubSpot sends {@code Retry-After} in seconds on a 429. Honouring it beats guessing: it is the portal's own
     * statement of when the window reopens.
     */
    private static @Nullable Duration retryAfter(@Nullable HttpHeaders headers) {
        if (headers == null) {
            return null;
        }
        var value = headers.getFirst(HttpHeaders.RETRY_AFTER);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Duration.ofSeconds(Long.parseLong(value.trim()));
        } catch (NumberFormatException e) {
            // The header also allows an HTTP date. Rare from HubSpot, and the backoff covers us.
            return null;
        }
    }

    private void throttle(@NonNull String account) {
        var interval = this.properties.minRequestInterval().toMillis();
        if (interval <= 0) {
            return;
        }

        var next = this.nextAllowedAt.computeIfAbsent(account, key -> new AtomicLong(0));
        while (true) {
            var now = System.currentTimeMillis();
            var previous = next.get();
            var start = Math.max(previous, now);
            var waitMs = start - now;
            if (waitMs > this.properties.maxBackoff().toMillis()) {
                // Claim nothing: a refused call must not push the calls behind it further back.
                throw new CrmUpstreamException(
                        "HubSpot's request quota for this account is saturated; retry in a few seconds");
            }
            // Atomically claim the slot [start, start + interval).
            if (next.compareAndSet(previous, start + interval)) {
                if (waitMs > 0) {
                    sleep(waitMs);
                }
                return;
            }
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InternalException("Interrupted while waiting for HubSpot", e);
        }
    }
}
