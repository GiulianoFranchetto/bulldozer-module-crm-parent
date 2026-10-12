package com.bulldozer_collective.modules.crm.gateway.hubspot;

import com.bulldozer_collective.logging.BulldozerLogger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * A 429 from HubSpot is expected traffic on any real portal, not an incident: it must not fail the caller's
 * request when a short wait fixes it. These tests pin the retry policy, including where it deliberately
 * refuses to retry.
 */
class HubSpotRequestExecutorTest {

    /** Zero waits: the policy is what is under test, not the clock. */
    private static final HubSpotProperties FAST = new HubSpotProperties(
            Duration.ZERO, 3, Duration.ZERO, Duration.ZERO);

    private final HubSpotRequestExecutor executor =
            new HubSpotRequestExecutor(FAST, mock(BulldozerLogger.class));

    @Test
    @DisplayName("a 429 is retried and the call eventually succeeds")
    void retriesOnTooManyRequests() {
        var attempts = new AtomicInteger();

        var result = this.executor.execute("test", true, () -> {
            if (attempts.incrementAndGet() < 3) {
                throw tooManyRequests(null);
            }
            return "ok";
        });

        assertThat(result).isEqualTo("ok");
        assertThat(attempts.get()).isEqualTo(3);
    }

    @Test
    @DisplayName("Retry-After is honoured rather than guessed at")
    void honoursRetryAfterHeader() {
        var attempts = new AtomicInteger();

        var result = this.executor.execute("test", true, () -> {
            if (attempts.incrementAndGet() == 1) {
                throw tooManyRequests("2");
            }
            return "ok";
        });

        assertThat(result).isEqualTo("ok");
        assertThat(attempts.get()).isEqualTo(2);
    }

    @Test
    @DisplayName("a transient 5xx is retried")
    void retriesOnServerError() {
        var attempts = new AtomicInteger();

        var result = this.executor.execute("test", true, () -> {
            if (attempts.incrementAndGet() == 1) {
                throw HttpServerErrorException.create(HttpStatus.BAD_GATEWAY, "bad gateway",
                        HttpHeaders.EMPTY, new byte[0], null);
            }
            return "ok";
        });

        assertThat(result).isEqualTo("ok");
        assertThat(attempts.get()).isEqualTo(2);
    }

    @Test
    @DisplayName("a 4xx other than 429 is not retried: the request is wrong and repeating it burns quota")
    void doesNotRetryOnClientError() {
        var attempts = new AtomicInteger();

        assertThatThrownBy(() -> this.executor.execute("test", true, () -> {
            attempts.incrementAndGet();
            throw HttpClientErrorException.create(HttpStatus.BAD_REQUEST, "bad request",
                    HttpHeaders.EMPTY, new byte[0], null);
        })).isInstanceOf(HttpClientErrorException.class);

        assertThat(attempts.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("retries are bounded — the caller is waiting on the other end")
    void givesUpAfterMaxRetries() {
        var attempts = new AtomicInteger();

        assertThatThrownBy(() -> this.executor.execute("test", true, () -> {
            attempts.incrementAndGet();
            throw tooManyRequests(null);
        })).isInstanceOf(HttpClientErrorException.class);

        // maxRetries = 3, so four attempts in total.
        assertThat(attempts.get()).isEqualTo(4);
    }

    @Test
    @DisplayName("a create is not replayed after a 5xx: HubSpot may have created the record already")
    void doesNotReplayNonIdempotentCallOnServerError() {
        var attempts = new AtomicInteger();

        assertThatThrownBy(() -> this.executor.execute("test", false, () -> {
            attempts.incrementAndGet();
            throw HttpServerErrorException.create(HttpStatus.BAD_GATEWAY, "bad gateway",
                    HttpHeaders.EMPTY, new byte[0], null);
        })).isInstanceOf(HttpServerErrorException.class);

        assertThat(attempts.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("a create is not replayed after a timeout: the request may have reached HubSpot")
    void doesNotReplayNonIdempotentCallOnTimeout() {
        var attempts = new AtomicInteger();

        assertThatThrownBy(() -> this.executor.execute("test", false, () -> {
            attempts.incrementAndGet();
            throw new ResourceAccessException("Read timed out");
        })).isInstanceOf(ResourceAccessException.class);

        assertThat(attempts.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("a create refused with a 429 is retried: HubSpot did nothing")
    void retriesNonIdempotentCallOnTooManyRequests() {
        var attempts = new AtomicInteger();

        var result = this.executor.execute("test", false, () -> {
            if (attempts.incrementAndGet() == 1) {
                throw tooManyRequests(null);
            }
            return "ok";
        });

        assertThat(result).isEqualTo("ok");
        assertThat(attempts.get()).isEqualTo(2);
    }

    private static HttpClientErrorException tooManyRequests(String retryAfterSeconds) {
        var headers = new HttpHeaders();
        if (retryAfterSeconds != null) {
            headers.set(HttpHeaders.RETRY_AFTER, retryAfterSeconds);
        }
        return HttpClientErrorException.create(
                HttpStatus.TOO_MANY_REQUESTS, "too many requests", headers, new byte[0], null);
    }
}
