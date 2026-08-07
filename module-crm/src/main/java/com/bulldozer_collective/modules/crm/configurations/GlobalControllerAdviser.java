package com.bulldozer_collective.modules.crm.configurations;

import com.bulldozer_collective.logging.BulldozerLogger;
import com.bulldozer_collective.modules.crm.sync.services.CrmSyncValidationException;
import com.bulldozer_collective.modules.crm.sync.services.NoTokenException;
import com.bulldozer_collective.shared.exceptions.BadRequestException;
import com.bulldozer_collective.shared.exceptions.BulldozerException;
import com.bulldozer_collective.shared.exceptions.BulldozerRuntimeException;
import com.bulldozer_collective.shared.exceptions.ConflictException;
import com.bulldozer_collective.shared.exceptions.ForbiddenException;
import com.bulldozer_collective.shared.exceptions.InternalException;
import com.bulldozer_collective.shared.exceptions.NotFoundException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import static com.bulldozer_collective.modules.crm.configurations.CrmLogContext.op;
import static com.bulldozer_collective.modules.crm.configurations.RequestErrorDescriber.describe;
import static com.bulldozer_collective.modules.crm.configurations.RequestErrorDescriber.resolveRequiredType;

/**
 * Maps exceptions to HTTP responses for the whole module.
 *
 * <p>Contract: every <b>4xx</b> carries a {@code {"error": "<message>"}} body naming what the caller got
 * wrong, because the caller is the frontend or an MCP agent and neither can self-correct from a bodyless
 * {@code 400 Bad Request}. Every <b>5xx</b> stays bodyless — an internal failure must be logged, not leaked.
 *
 * <p>Mirrors {@code module-outbound}'s adviser, the reference implementation of this contract.
 */
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalControllerAdviser {

    private final BulldozerLogger logger;

    // ── 404 ──────────────────────────────────────────────────────────────────

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Void> on(NotFoundException e) {
        this.logger.debug(op("CRM-006") + "Not found: %s", e.getMessage());
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Void> on(EntityNotFoundException e) {
        this.logger.debug(op("CRM-006") + "Entity not found: %s", e.getMessage());
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Void> on(NoResourceFoundException e) {
        this.logger.debug(op("CRM-006") + "No resource: %s", e.getMessage());
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Void> on(NoSuchElementException e) {
        this.logger.debug(op("CRM-006") + "No resource: %s", e.getMessage());
        return ResponseEntity.notFound().build();
    }

    // ── 400 ──────────────────────────────────────────────────────────────────

    /**
     * Binding or validation failed on a whole argument object: either a conversion failure while binding the
     * query string onto a {@code *Param} record (e.g. {@code ListCrmSyncRunsParam.status} receiving a value
     * outside {@code CrmSyncStatus}), or a jakarta constraint violation on a {@code @Valid @RequestBody}
     * ({@code MethodArgumentNotValidException} extends {@link BindException}).
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Map<String, String>> on(BindException e) {
        var message = e.getFieldErrors().stream()
                // isBindingFailure() separates "could not convert the value" from "value is invalid".
                .map(fieldError -> fieldError.isBindingFailure()
                        ? describe(fieldError.getField(),
                                   fieldError.getRejectedValue(),
                                   resolveRequiredType(e.getBindingResult(), fieldError))
                        : fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.joining("; "));

        this.logger.warn(op("CRM-006") + "Parameter binding failed: %s", message);
        return ResponseEntity.badRequest().body(error(message, "Invalid request parameters"));
    }

    /**
     * A path variable or query parameter could not be converted to its declared type — a malformed UUID, or a
     * value outside an enum. Not an {@link IllegalArgumentException} ({@link TypeMismatchException} sits under
     * {@code BeansException}), so without this handler a bad {@code /crm/contacts/{id}} answered an opaque 500
     * on what is a client error.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> on(MethodArgumentTypeMismatchException e) {
        var message = describe(e.getName(), e.getValue(), e.getRequiredType());
        this.logger.warn(op("CRM-006") + "Parameter type mismatch: %s", message);
        return ResponseEntity.badRequest().body(Map.of("error", message));
    }

    /** Conversion failures that do not reach us as a {@link MethodArgumentTypeMismatchException}. */
    @ExceptionHandler(TypeMismatchException.class)
    public ResponseEntity<Map<String, String>> on(TypeMismatchException e) {
        var name = e.getPropertyName() != null ? e.getPropertyName() : "request";
        var message = describe(name, e.getValue(), e.getRequiredType());
        this.logger.warn(op("CRM-006") + "Type mismatch: %s", message);
        return ResponseEntity.badRequest().body(Map.of("error", message));
    }

    /**
     * The request body could not be parsed — malformed JSON, or a value outside an enum inside the body (e.g.
     * an unknown {@code platform} on {@code POST /crm/connections}). Jackson's own message names the offending
     * field and the accepted constants, so it is worth surfacing.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> on(HttpMessageNotReadableException e) {
        var cause = e.getMostSpecificCause().getMessage();
        this.logger.warn(op("CRM-006") + "Unreadable request body: %s", cause);
        return ResponseEntity.badRequest()
                .body(Map.of("error", "Malformed request body" + (blank(cause) ? "" : ": " + cause)));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, String>> on(MissingServletRequestParameterException e) {
        var message = "%s is required (expected a %s)".formatted(e.getParameterName(), e.getParameterType());
        this.logger.warn(op("CRM-006") + "Missing request parameter: %s", message);
        return ResponseEntity.badRequest().body(Map.of("error", message));
    }

    /** Constraints declared directly on handler-method parameters (Spring's method validation). */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<Map<String, String>> on(HandlerMethodValidationException e) {
        var message = e.getAllErrors().stream()
                .map(resolvable -> resolvable.getDefaultMessage())
                .filter(defaultMessage -> !blank(defaultMessage))
                .collect(Collectors.joining("; "));

        this.logger.warn(op("CRM-006") + "Method validation failed: %s", message);
        return ResponseEntity.badRequest().body(error(message, "Invalid request parameters"));
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Map<String, String>> on(ValidationException e) {
        this.logger.warn(op("CRM-006") + "Validation exception: %s", e.getMessage());
        return ResponseEntity.badRequest().body(error(e.getMessage(), "Invalid request parameters"));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Map<String, String>> on(BadRequestException e) {
        this.logger.warn(op("CRM-006") + "Bad request: %s", e.getMessage());
        return ResponseEntity.badRequest().body(error(e.getMessage(), "Bad request"));
    }

    /**
     * Unmapped pipeline stages block a sync. The message lists every offending stage, and the caller fixes it
     * by creating the missing mappings — so it must reach them.
     */
    @ExceptionHandler(CrmSyncValidationException.class)
    public ResponseEntity<Map<String, String>> on(CrmSyncValidationException e) {
        this.logger.warn(op("CRM-006") + "CRM sync validation failed: %s", e.getMessage());
        return ResponseEntity.badRequest().body(error(e.getMessage(), "Some pipeline stages are not mapped"));
    }

    /**
     * The module's own argument checks (e.g. a platform that is not a CRM, a stage id from another project)
     * are thrown as {@link IllegalArgumentException} with a caller-facing message — surface it.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> on(IllegalArgumentException e) {
        this.logger.warn(op("CRM-006") + "Illegal argument: %s", e.getMessage(), e);
        return ResponseEntity.badRequest().body(error(e.getMessage(), "Invalid request"));
    }

    /**
     * Unlike {@link IllegalArgumentException}, an {@link IllegalStateException} message describes our internal
     * state and must not be sent to the caller — answer a generic body and keep the detail in the logs.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> on(IllegalStateException e) {
        this.logger.error(op("CRM-006") + "Illegal state", e);
        return ResponseEntity.badRequest()
                .body(Map.of("error", "The request could not be processed in the current state"));
    }

    // ── 424 ──────────────────────────────────────────────────────────────────

    /**
     * The project has no OAuth token for that CRM. The caller fixes it by running the OAuth flow, so name the
     * platform. 424 rather than 400: the request was well formed, a prerequisite is missing.
     */
    @ExceptionHandler(NoTokenException.class)
    public ResponseEntity<Map<String, String>> on(NoTokenException e) {
        this.logger.warn(op("CRM-006") + "No CRM token: %s", e.getMessage());
        return ResponseEntity.status(424).body(error(e.getMessage(), "No token for this CRM platform"));
    }

    // ── 403 ──────────────────────────────────────────────────────────────────

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Map<String, String>> on(ForbiddenException e) {
        this.logger.warn(op("CRM-006") + "Forbidden: %s", e.getMessage());
        return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));
    }

    // ── 409 ──────────────────────────────────────────────────────────────────

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Map<String, String>> on(ConflictException e) {
        this.logger.warn(op("CRM-006") + "Conflict: %s", e.getMessage());
        return ResponseEntity.status(409).body(error(e.getMessage(), "Conflict"));
    }

    // ── 501 ──────────────────────────────────────────────────────────────────

    /** Salesforce and Pipedrive adapters are not implemented yet. */
    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<Map<String, String>> on(UnsupportedOperationException e) {
        this.logger.warn(op("CRM-006") + "Not implemented: %s", e.getMessage());
        return ResponseEntity.status(501).body(error(e.getMessage(), "Not implemented yet"));
    }

    // ── 502 ──────────────────────────────────────────────────────────────────

    @ExceptionHandler(HttpClientErrorException.class)
    public ResponseEntity<Void> on(HttpClientErrorException e) {
        // An upstream dependency (internal module / CRM API) returned an error — answer a stable 502 rather
        // than leaking the downstream status to our caller.
        this.logger.warn(op("CRM-006") + "Upstream dependency returned %s", e.getStatusCode());
        return ResponseEntity.status(502).build();
    }

    // ── 500 ──────────────────────────────────────────────────────────────────
    // Bodyless on purpose: internal failure detail stays in the logs.

    @ExceptionHandler(InternalException.class)
    public ResponseEntity<Void> on(InternalException e) {
        this.logger.error(op("CRM-006") + "Internal error", e);
        return ResponseEntity.internalServerError().build();
    }

    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<Void> on(NullPointerException e) {
        this.logger.error(op("CRM-006") + "Null pointer", e);
        return ResponseEntity.internalServerError().build();
    }

    @ExceptionHandler(BulldozerRuntimeException.class)
    public ResponseEntity<Void> on(BulldozerRuntimeException e) {
        this.logger.error(op("CRM-006") + "Unhandled Bulldozer runtime exception", e);
        return ResponseEntity.internalServerError().build();
    }

    @ExceptionHandler(BulldozerException.class)
    public ResponseEntity<Void> on(BulldozerException e) {
        this.logger.error(op("CRM-006") + "Unhandled Bulldozer exception", e);
        return ResponseEntity.internalServerError().build();
    }

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<Void> on(Throwable e) {
        this.logger.error(op("CRM-006") + "Unhandled exception: %s", e.getClass().getName(), e);
        return ResponseEntity.internalServerError().build();
    }

    /** Never answer {@code {"error": null}} — fall back to a stable sentence when the exception has no message. */
    private static Map<String, String> error(@Nullable String message, @NonNull String fallback) {
        return Map.of("error", blank(message) ? fallback : message);
    }

    private static boolean blank(@Nullable String value) {
        return value == null || value.isBlank();
    }
}
