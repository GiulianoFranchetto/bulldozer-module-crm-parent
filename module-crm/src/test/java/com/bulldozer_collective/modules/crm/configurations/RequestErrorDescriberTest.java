package com.bulldozer_collective.modules.crm.configurations;

import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Bug 58 — a rejected request value must come back as a message the caller can act on. These cases are the
 * contract the {@code GlobalControllerAdviser} relies on; they run without a Spring context.
 */
class RequestErrorDescriberTest {

    private enum SampleState {
        SAMPLE_STATE_OPENED,
        SAMPLE_STATE_ARCHIVED,
        @Deprecated
        SAMPLE_STATE_LEGACY
    }

    private record SampleParam(SampleState state, UUID accountId) {}

    @Test
    void namesTheFieldAndListsTheAcceptedConstantsOfAnEnum() {
        var message = RequestErrorDescriber.describe("state", "SAMPLE_STATE_BOGUS", SampleState.class);

        assertEquals("state: 'SAMPLE_STATE_BOGUS' is not a valid value; "
                + "accepted values: SAMPLE_STATE_OPENED, SAMPLE_STATE_ARCHIVED", message);
    }

    @Test
    void hidesDeprecatedConstantsFromTheAcceptedList() {
        var message = RequestErrorDescriber.describe("state", "x", SampleState.class);

        assertFalse(message.contains("SAMPLE_STATE_LEGACY"),
                "a deprecated constant must not be advertised as a value to send");
    }

    @Test
    void describesANonEnumTypeByItsSimpleName() {
        var message = RequestErrorDescriber.describe("limit", "abc", Integer.class);

        assertEquals("limit: expected a value of type Integer, got 'abc'", message);
    }

    @Test
    void degradesGracefullyWhenTheTargetTypeIsUnknown() {
        var message = RequestErrorDescriber.describe("state", "bogus", null);

        assertTrue(message.startsWith("state:"), "the field name is the minimum the caller needs");
    }

    @Test
    void resolvesTheEnumTypeFromTheBindingResultSoTheConstantsCanBeListed() {
        var target = new SampleParam(null, null);
        var result = new BeanPropertyBindingResult(target, "sampleParam");
        var error = new FieldError("sampleParam", "state", "SAMPLE_STATE_BOGUS", true, null, null, null);

        assertEquals(SampleState.class, RequestErrorDescriber.resolveRequiredType(result, error));
    }

    @Test
    void neverThrowsWhenTheTargetCannotBeInspected() {
        var result = new BeanPropertyBindingResult(null, "sampleParam");
        var error = new FieldError("sampleParam", "state", "SAMPLE_STATE_BOGUS", true, null, null, null);

        assertNull(RequestErrorDescriber.resolveRequiredType(result, error),
                "an adviser must not fail while reporting a failure");
    }
}
