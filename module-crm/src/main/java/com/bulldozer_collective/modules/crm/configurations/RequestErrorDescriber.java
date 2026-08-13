package com.bulldozer_collective.modules.crm.configurations;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Turns a rejected request value into a message the caller can act on. The caller of a listing endpoint is
 * usually the frontend or an MCP agent: a bodyless {@code 400} leaves it guessing, so an invalid enum must name
 * the field and list the accepted constants.
 */
public final class RequestErrorDescriber {
    private RequestErrorDescriber() {}

    /**
     * {@code "status: 'BOGUS' is not a valid value; accepted values: CRM_SYNC_STATUS_RUNNING,
     * CRM_SYNC_STATUS_SUCCESS, CRM_SYNC_STATUS_PARTIAL, CRM_SYNC_STATUS_FAILED"}
     *
     * @param requiredType the type the value should have been converted to, or {@code null} when the
     *                     framework did not report one — the message then degrades to naming the field only.
     */
    public static String describe(@NonNull String field,
                                  @Nullable Object value,
                                  @Nullable Class<?> requiredType) {
        if (requiredType != null && requiredType.isEnum()) {
            return "%s: '%s' is not a valid value; accepted values: %s"
                    .formatted(field, value, acceptedValues(requiredType));
        }
        if (UUID.class.equals(requiredType)) {
            return "%s: expected a UUID, got '%s'".formatted(field, value);
        }
        if (requiredType != null) {
            return "%s: expected a value of type %s, got '%s'"
                    .formatted(field, requiredType.getSimpleName(), value);
        }
        return "%s: invalid value '%s'".formatted(field, value);
    }

    /**
     * Describes a value that failed to bind onto a {@code *Param} record. Use this rather than
     * {@link #describe} for a {@code BindException} field error: it is the only entry point that can see the
     * element type of a collection parameter.
     *
     * <p>Bug 70 — {@code sources=["ATLANTIS"]} on {@code GET /seo/domains/{id}/keywords} used to answer
     * {@code "sources: expected a value of type List, got 'ATLANTIS'"}: the declared type of the field is
     * {@code List<SeoDataSource>}, so the enum branch of {@link #describe} was never reached and the accepted
     * constants — the whole point of the message — were lost, while the message wrongly told the caller its
     * list was not a list. Spring reports the failure as a {@code TypeMismatchException} on the whole list,
     * whose cause chain carries a {@link ConversionFailedException} naming the element type and the offending
     * element; the rejected value is the elements joined by commas. Falls back to {@link #describe} whenever
     * that chain does not describe an enum element, so the previous message stays the worst case.
     */
    public static String describeBindingFailure(@NonNull BindingResult result, @NonNull FieldError error) {
        var requiredType = resolveRequiredType(result, error);
        var elementMessage = describeElementFailure(error, requiredType);
        return elementMessage != null
                ? elementMessage
                : describe(error.getField(), error.getRejectedValue(), requiredType);
    }

    /**
     * {@code "sources[1]: 'ATLANTIS' is not a valid value; accepted values: FRANCE, …"}, or {@code null} when
     * the failure is not an enum element inside a collection. Never throws: an adviser must not fail while
     * reporting a failure.
     */
    private static @Nullable String describeElementFailure(@NonNull FieldError error,
                                                           @Nullable Class<?> requiredType) {
        try {
            // A scalar enum is already handled by describe(); only a collection needs the element type.
            if (requiredType != null && requiredType.isEnum()) {
                return null;
            }
            var elementType = failedElementType(error);
            if (elementType == null || !elementType.isEnum()) {
                return null;
            }

            // Only a collection carries positions; a scalar whose declared type stayed unresolved must not be
            // reported as "field[0]".
            var indexable = error.getField().endsWith("]")
                    || (requiredType != null
                        && (Collection.class.isAssignableFrom(requiredType) || requiredType.isArray()));
            var constants = Arrays.stream(elementType.getEnumConstants())
                    .map(constant -> ((Enum<?>) constant).name())
                    .collect(Collectors.toSet());
            var offenders = indexable
                    ? offenders(error.getField(), elements(error.getRejectedValue()), constants)
                    : List.<String>of();
            if (offenders.isEmpty()) {
                // Not a collection, or a rejected value that could not be split back into its elements — name
                // the single value Spring rejected.
                var rejected = failedElementValue(error);
                return rejected == null
                        ? null
                        : "%s: '%s' is not a valid value; accepted values: %s"
                                .formatted(error.getField(), rejected, acceptedValues(elementType));
            }
            return "%s %s; accepted values: %s".formatted(
                    String.join(", ", offenders),
                    offenders.size() == 1 ? "is not a valid value" : "are not valid values",
                    acceptedValues(elementType));
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    /** {@code ["sources[1]: 'ATLANTIS'", "sources[3]: 'BOGUS'"]} — every element outside the enum. */
    private static List<String> offenders(@NonNull String field,
                                          @Nullable List<String> elements,
                                          @NonNull Set<String> constants) {
        if (elements == null) {
            return List.of();
        }
        // An already-indexed field name (sources[0]) must not be indexed twice.
        var indexed = field.endsWith("]");
        return IntStream.range(0, elements.size())
                .filter(index -> !constants.contains(elements.get(index).trim()))
                .mapToObj(index -> indexed
                        ? "%s: '%s'".formatted(field, elements.get(index))
                        : "%s[%d]: '%s'".formatted(field, index, elements.get(index)))
                .toList();
    }

    /** The rejected value of a collection field reaches us joined by commas, or as the raw array/collection. */
    private static @Nullable List<String> elements(@Nullable Object value) {
        if (value instanceof Object[]) {
            return Arrays.stream((Object[]) value).map(String::valueOf).toList();
        }
        if (value instanceof Collection<?>) {
            return ((Collection<?>) value).stream().map(String::valueOf).toList();
        }
        if (value instanceof String) {
            return Arrays.stream(((String) value).split(",")).toList();
        }
        return null;
    }

    /**
     * The element type Spring failed to convert to, read from the innermost {@link ConversionFailedException}
     * of the cause chain — the outer failure only knows the field is a {@code List}.
     */
    private static @Nullable Class<?> failedElementType(@NonNull FieldError error) {
        var failure = conversionFailure(error);
        return failure == null ? null : failure.getTargetType().getType();
    }

    private static @Nullable Object failedElementValue(@NonNull FieldError error) {
        var failure = conversionFailure(error);
        return failure == null ? null : failure.getValue();
    }

    private static @Nullable ConversionFailedException conversionFailure(@NonNull FieldError error) {
        if (!error.contains(Throwable.class)) {
            return null;
        }
        ConversionFailedException deepest = null;
        var cause = error.unwrap(Throwable.class);
        for (var depth = 0; cause != null && depth < MAX_CAUSE_DEPTH; cause = cause.getCause(), depth++) {
            if (cause instanceof ConversionFailedException failure) {
                deepest = failure;
            }
        }
        return deepest;
    }

    /** Guards against a self-referencing cause chain — an adviser must not spin while reporting a failure. */
    private static final int MAX_CAUSE_DEPTH = 16;

    /**
     * Deprecated constants are hidden: the caller reads this list as "what I should send". Falls back to
     * every constant if they are all deprecated, so the list is never empty.
     */
    private static String acceptedValues(@NonNull Class<?> enumType) {
        var live = Arrays.stream(enumType.getEnumConstants())
                .map(constant -> (Enum<?>) constant)
                .filter(constant -> !isDeprecated(enumType, constant))
                .map(Enum::name)
                .collect(Collectors.joining(", "));
        return live.isEmpty()
                ? Arrays.stream(enumType.getEnumConstants())
                        .map(constant -> ((Enum<?>) constant).name())
                        .collect(Collectors.joining(", "))
                : live;
    }

    private static boolean isDeprecated(@NonNull Class<?> enumType, @NonNull Enum<?> constant) {
        try {
            return enumType.getField(constant.name()).isAnnotationPresent(Deprecated.class);
        } catch (NoSuchFieldException | RuntimeException ignored) {
            return false;
        }
    }

    /**
     * A {@link FieldError} carries the field name and the rejected value but not the target type. Recover it
     * from the binding result, then from the wrapped {@link TypeMismatchException}, then by reflecting the
     * target's record components — the last resort matters because the whole point of the message is to name
     * the enum's constants, and an unresolved type loses them. Never throws: an adviser must not fail while
     * reporting a failure.
     */
    public static @Nullable Class<?> resolveRequiredType(@NonNull BindingResult result,
                                                         @NonNull FieldError error) {
        try {
            var fieldType = result.getFieldType(error.getField());
            if (fieldType != null) {
                return fieldType;
            }
        } catch (RuntimeException ignored) {
            // getFieldType() walks the target via a BeanWrapper; a half-constructed record can make it throw.
        }

        if (error.contains(TypeMismatchException.class)) {
            var requiredType = error.unwrap(TypeMismatchException.class).getRequiredType();
            if (requiredType != null) {
                return requiredType;
            }
        }

        return recordComponentType(result.getTarget(), error.getField());
    }

    private static @Nullable Class<?> recordComponentType(@Nullable Object target, @NonNull String field) {
        if (target == null || !target.getClass().isRecord()) {
            return null;
        }
        // Spring names an element failure "sources[0]", which never equals the record component "sources".
        var name = field.contains("[") ? field.substring(0, field.indexOf('[')) : field;
        return Arrays.stream(target.getClass().getRecordComponents())
                .filter(component -> component.getName().equals(name))
                .map(RecordComponent::getType)
                .findFirst()
                .orElse(null);
    }
}
