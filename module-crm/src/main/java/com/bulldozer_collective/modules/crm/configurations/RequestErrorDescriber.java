package com.bulldozer_collective.modules.crm.configurations;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.TypeMismatchException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

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
        return Arrays.stream(target.getClass().getRecordComponents())
                .filter(component -> component.getName().equals(field))
                .map(RecordComponent::getType)
                .findFirst()
                .orElse(null);
    }
}
