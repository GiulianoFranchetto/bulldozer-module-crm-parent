package com.bulldozer_collective.modules.crm.configurations;

import org.junit.jupiter.api.Test;
import org.springframework.core.ResolvableType;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.DataBinder;
import org.springframework.validation.FieldError;

import java.util.List;
import java.util.Map;
import java.util.Set;
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

    private record SampleListParam(List<SampleState> states) {}

    /** Binds a query string onto a record the way Spring MVC does, so the field errors are the real ones. */
    private static BindingResult bind(Class<?> paramType, Map<String, Object> values) {
        var binder = new DataBinder(null, "param");
        binder.setConversionService(new DefaultFormattingConversionService());
        binder.setTargetType(ResolvableType.forClass(paramType));
        binder.construct(new DataBinder.ValueResolver() {
            @Override
            public Object resolveValue(String name, Class<?> type) {
                return values.get(name);
            }

            @Override
            public Set<String> getNames() {
                return values.keySet();
            }
        });
        return binder.getBindingResult();
    }

    private static String describeFirstBindingFailure(Class<?> paramType, Map<String, Object> values) {
        var result = bind(paramType, values);
        var error = result.getFieldErrors().stream()
                .filter(FieldError::isBindingFailure)
                .findFirst()
                .orElseThrow(() -> new AssertionError("expected a binding failure, got " + result.getFieldErrors()));
        return RequestErrorDescriber.describeBindingFailure(result, error);
    }

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

    // ── Bug 70 — an invalid element of a List<Enum> parameter ────────────────────────────────────────────

    @Test
    void namesTheOffendingElementOfAnEnumListAndListsTheAcceptedConstants() {
        var message = describeFirstBindingFailure(
                SampleListParam.class, Map.of("states", new String[] {"SAMPLE_STATE_BOGUS"}));

        assertEquals("states[0]: 'SAMPLE_STATE_BOGUS' is not a valid value; "
                + "accepted values: SAMPLE_STATE_OPENED, SAMPLE_STATE_ARCHIVED", message);
    }

    @Test
    void indexesTheOffendingElementAmongTheValidOnes() {
        var message = describeFirstBindingFailure(
                SampleListParam.class, Map.of("states", new String[] {"SAMPLE_STATE_OPENED", "SAMPLE_STATE_BOGUS"}));

        assertTrue(message.startsWith("states[1]: 'SAMPLE_STATE_BOGUS' is not a valid value;"), message);
    }

    @Test
    void reportsEveryOffendingElementNotOnlyTheOneSpringStoppedOn() {
        var message = describeFirstBindingFailure(
                SampleListParam.class, Map.of("states", new String[] {"NOPE", "SAMPLE_STATE_OPENED", "NEITHER"}));

        assertEquals("states[0]: 'NOPE', states[2]: 'NEITHER' are not valid values; "
                + "accepted values: SAMPLE_STATE_OPENED, SAMPLE_STATE_ARCHIVED", message);
    }

    @Test
    void acceptsTheCommaJoinedFormOfARepeatedQueryParameter() {
        var message = describeFirstBindingFailure(
                SampleListParam.class, Map.of("states", "SAMPLE_STATE_OPENED,SAMPLE_STATE_BOGUS"));

        assertTrue(message.startsWith("states[1]: 'SAMPLE_STATE_BOGUS' is not a valid value;"), message);
    }

    @Test
    void hidesDeprecatedConstantsFromTheAcceptedListOfAnElementToo() {
        var message = describeFirstBindingFailure(
                SampleListParam.class, Map.of("states", new String[] {"SAMPLE_STATE_BOGUS"}));

        assertFalse(message.contains("SAMPLE_STATE_LEGACY"),
                "a deprecated constant must not be advertised as a value to send");
    }

    @Test
    void stillDescribesAScalarEnumTheOldWayWhenBindingAWholeRecord() {
        var message = describeFirstBindingFailure(
                SampleParam.class, Map.of("state", "SAMPLE_STATE_BOGUS"));

        assertEquals("state: 'SAMPLE_STATE_BOGUS' is not a valid value; "
                + "accepted values: SAMPLE_STATE_OPENED, SAMPLE_STATE_ARCHIVED", message);
    }

    @Test
    void stillDescribesANonEnumFailureTheOldWay() {
        var message = describeFirstBindingFailure(
                SampleParam.class, Map.of("accountId", "not-a-uuid"));

        assertEquals("accountId: expected a UUID, got 'not-a-uuid'", message);
    }

    @Test
    void fallsBackToTheDeclaredTypeWhenTheFailureIsNotAnEnumElement() {
        var result = new BeanPropertyBindingResult(null, "sampleParam");
        var error = new FieldError("sampleParam", "states", "SAMPLE_STATE_BOGUS", true, null, null, null);

        assertEquals("states: invalid value 'SAMPLE_STATE_BOGUS'",
                RequestErrorDescriber.describeBindingFailure(result, error));
    }
}
