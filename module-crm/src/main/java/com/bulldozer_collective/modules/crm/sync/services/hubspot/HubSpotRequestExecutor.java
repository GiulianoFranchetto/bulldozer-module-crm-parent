package com.bulldozer_collective.modules.crm.sync.services.hubspot;

import com.bulldozer_collective.logging.BulldozerLogger;
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
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import static com.bulldozer_collective.modules.crm.configurations.CrmLogContext.op;

/**
 * Throttles and retries every HubSpot call.
 *
 * <p>Without this, one {@code 429} — which a portal of any size will produce — propagates out of the sync task
 * and fails the whole sync until the next scheduled run twelve hours later. A transient {@code 5xx} or a read
 * timeout did the same.
 *
 * <p>Two mechanisms, in this order:
 * <ol>
 *   <li>a throttle shared across threads, so the aggregate rate stays under HubSpot's quota instead of relying
 *       on a per-thread sleep (which bounds nothing once more than one sync runs);</li>
 *   <li>a bounded retry that honours HubSpot's {@code Retry-After} header and otherwise backs off
 *       exponentially. Bounded on purpose — the job library has no attempt counter, so an unbounded retry
 *       inside a task turns into an infinite loop against a permanently broken portal.</li>
 * </ol>
 *
 * <p>A {@code 4xx} other than {@code 429} is not retried: the request is wrong, and repeating it wastes quota.
 */
@Component
@RequiredArgsConstructor
public class HubSpotRequestExecutor {

    private final HubSpotProperties properties;
    private final BulldozerLogger logger;

    /** Next epoch-milli at which a call is allowed; shared by every thread. */
    private final AtomicLong nextAllowedAt = new AtomicLong(0);

    public <T> T execute(@NonNull String description, @NonNull Supplier<T> call) {
        var attempts = this.properties.maxRetries() + 1;
        RuntimeException last = null;

        for (var attempt = 1; attempt <= attempts; attempt++) {
            this.throttle();
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
                // 5xx and read timeouts are the transient cases worth another attempt.
                last = e;
                this.pause(description, attempt, null);
            }
        }

        this.logger.error(op("CRM-002") + "HubSpot call '%s' still failing after %d attempts",
                description, attempts);
        throw last != null ? last : new IllegalStateException("HubSpot call failed: " + description);
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

    private void throttle() {
        var interval = this.properties.minRequestInterval().toMillis();
        if (interval <= 0) {
            return;
        }

        var now = System.currentTimeMillis();
        // Atomically claim the next slot: max(now, previous slot) + interval.
        var slot = this.nextAllowedAt.accumulateAndGet(now,
                (previous, current) -> Math.max(previous, current) + interval);

        var waitMs = slot - interval - now;
        if (waitMs > 0) {
            sleep(waitMs);
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for HubSpot", e);
        }
    }
}
