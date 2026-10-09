package com.bulldozer_collective.modules.crm.configurations;

import com.bulldozer_collective.logging.BulldozerLogger;
import com.bulldozer_collective.modules.crm.gateway.CrmAuthorizationException;
import com.bulldozer_collective.modules.crm.gateway.NoTokenException;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import static com.bulldozer_collective.modules.crm.configurations.CrmLogContext.op;
import static com.bulldozer_collective.modules.crm.configurations.RequestErrorDescriber.describe;
import static com.bulldozer_collective.modules.crm.configurations.RequestErrorDescriber.describeBindingFailure;

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
     * query string onto a {@code *Param} record (e.g. {@code ListCrmActivitiesParam.type} receiving a value
     * outside {@code CrmActivityType}), or a jakarta constraint violation on a {@code @Valid @RequestBody}
     * ({@code MethodArgumentNotValidException} extends {@link BindException}).
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Map<String, String>> on(BindException e) {
        var message = e.getFieldErrors().stream()
                // isBindingFailure() separates "could not convert the value" from "value is invalid".
                .map(fieldError -> fieldError.isBindingFailure()
                        ? describeBindingFailure(e.getBindingResult(), fieldError)
                        : fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.joining("; "));

        this.logger.warn(op("CRM-006") + "Parameter binding failed: %s", message);
        return ResponseEntity.badRequest().body(error(message, "Invalid request parameters"));
    }

    /**
     * A path variable or query parameter could not be converted to its declared type — a malformed UUID, or a
     * value outside an enum. Not an {@link IllegalArgumentException} ({@link TypeMismatchException} sits under
     * {@code BeansException}), so without this handler a bad {@code /crm/{connectionId}/contacts} answered an opaque 500
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
     * The module's own argument checks (e.g. a platform that is not a CRM, a stage with no mapping in the
     * deal's pipeline) are thrown as {@link IllegalArgumentException} with a caller-facing message — surface it.
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

    /** The CRM refused the project's token. Same remedy as a missing one: reconnect the CRM. */
    @ExceptionHandler(CrmAuthorizationException.class)
    public ResponseEntity<Map<String, String>> on(CrmAuthorizationException e) {
        this.logger.warn(op("CRM-006") + "CRM refused the token: %s", e.getMessage());
        return ResponseEntity.status(424).body(error(e.getMessage(), "The CRM refused the project's token"));
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

    /** The Salesforce gateway is not implemented yet. */
    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<Map<String, String>> on(UnsupportedOperationException e) {
        this.logger.warn(op("CRM-006") + "Not implemented: %s", e.getMessage());
        return ResponseEntity.status(501).body(error(e.getMessage(), "Not implemented yet"));
    }

    // ── 502 ──────────────────────────────────────────────────────────────────

    @ExceptionHandler({HttpClientErrorException.class, HttpServerErrorException.class, ResourceAccessException.class})
    public ResponseEntity<Void> on(RestClientException e) {
        // An upstream dependency (internal module / CRM API) failed in a way the gateway does not translate — a
        // 429 or a 5xx still failing after the retries, a timeout. Answer a stable 502 rather than leaking the
        // downstream status to our caller.
        this.logger.warn(op("CRM-006") + "Upstream dependency failed: %s", e.getMessage());
        return ResponseEntity.status(502).build();
    }

    // ── négociation de contenu / méthode ─────────────────────────────────────

    /*
     * Without these three, the @ExceptionHandler(Throwable.class) catch-all below wins over Spring's own
     * handling of its ErrorResponse exceptions, and a plain content-negotiation mistake answers an opaque
     * 500 instead of the 415 / 405 / 406 that tells the caller what to fix. The classic case: a POST sent
     * with the wrong Content-Type, or none at all — no mapping matches and the caller deserves to be told
     * which media types are accepted.
     */

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Map<String, String>> on(HttpMediaTypeNotSupportedException e) {
        var supported = e.getSupportedMediaTypes().stream().map(Object::toString).collect(Collectors.joining(", "));
        var message = "Unsupported Content-Type" + (blank(supported) ? "" : "; expected one of: " + supported);
        this.logger.warn(op("CRM-006") + "Unsupported media type: %s", message);
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(Map.of("error", message));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, String>> on(HttpRequestMethodNotSupportedException e) {
        var supported = e.getSupportedHttpMethods() == null
                ? ""
                : e.getSupportedHttpMethods().stream().map(Object::toString).collect(Collectors.joining(", "));
        var message = "Method " + e.getMethod() + " is not supported"
                + (blank(supported) ? "" : "; allowed: " + supported);
        this.logger.warn(op("CRM-006") + "Method not supported: %s", message);
        var response = ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED);
        // Keep the Allow header the catch-all was dropping — it is what the spec requires on a 405.
        if (!blank(supported)) {
            response = response.header(HttpHeaders.ALLOW, supported);
        }
        return response.body(Map.of("error", message));
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<Map<String, String>> on(HttpMediaTypeNotAcceptableException e) {
        this.logger.warn(op("CRM-006") + "Not acceptable: %s", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE)
                .body(Map.of("error", "No representation available for the requested Accept types"));
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
