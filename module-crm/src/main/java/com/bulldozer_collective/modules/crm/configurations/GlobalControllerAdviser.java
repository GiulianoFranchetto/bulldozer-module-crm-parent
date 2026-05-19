package com.bulldozer_collective.modules.crm.configurations;

import com.bulldozer_collective.logging.BulldozerLogger;
import com.bulldozer_collective.modules.crm.sync.services.CrmSyncValidationException;
import com.bulldozer_collective.shared.exceptions.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalControllerAdviser {
    private final BulldozerLogger logger;

    // ── 404 ──────────────────────────────────────────────────────────────────

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Void> on(NotFoundException e) {
        this.logger.debug("Not found: %s", e.getMessage());
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Void> on(EntityNotFoundException e) {
        this.logger.debug("Entity not found: %s", e.getMessage());
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Void> on(NoResourceFoundException e) {
        this.logger.debug("No resource: %s", e.getMessage());
        return ResponseEntity.notFound().build();
    }

    // ── 400 ──────────────────────────────────────────────────────────────────

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Void> on(BadRequestException e) {
        this.logger.warn("Bad request: %s", e.getMessage());
        return ResponseEntity.badRequest().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Void> on(IllegalArgumentException e) {
        this.logger.warn("Illegal argument: %s", e.getMessage());
        return ResponseEntity.badRequest().build();
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Void> on(IllegalStateException e) {
        this.logger.error("Illegal state", e);
        return ResponseEntity.badRequest().build();
    }

    @ExceptionHandler(CrmSyncValidationException.class)
    public ResponseEntity<Void> on(CrmSyncValidationException e) {
        this.logger.warn("CRM sync validation failed: %s", e.getMessage());
        return ResponseEntity.badRequest().build();
    }

    // ── 402 ──────────────────────────────────────────────────────────────────

    @ExceptionHandler(NotEnoughJetonException.class)
    public ResponseEntity<Void> on(NotEnoughJetonException e) {
        this.logger.warn("Not enough jetons: %s", e.getMessage());
        return ResponseEntity.status(402).build();
    }

    // ── 403 ──────────────────────────────────────────────────────────────────

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Void> on(ForbiddenException e) {
        this.logger.warn("Forbidden: %s", e.getMessage());
        return ResponseEntity.status(403).build();
    }

    @ExceptionHandler(ReadOnlyEntityException.class)
    public ResponseEntity<Void> on(ReadOnlyEntityException e) {
        this.logger.warn("Read-only entity: %s", e.getMessage());
        return ResponseEntity.status(403).build();
    }

    // ── 409 ──────────────────────────────────────────────────────────────────

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Void> on(ConflictException e) {
        this.logger.warn("Conflict: %s", e.getMessage());
        return ResponseEntity.status(409).build();
    }

    // ── 429 ──────────────────────────────────────────────────────────────────

    @ExceptionHandler(RateLimiterException.class)
    public ResponseEntity<Void> on(RateLimiterException e) {
        this.logger.warn("Rate limit exceeded: %s", e.getMessage());
        return ResponseEntity.status(429).build();
    }

    // ── 500 ──────────────────────────────────────────────────────────────────

    @ExceptionHandler(OAuthException.class)
    public ResponseEntity<Void> on(OAuthException e) {
        this.logger.error("OAuth error", e);
        return ResponseEntity.internalServerError().build();
    }

    @ExceptionHandler(InternalException.class)
    public ResponseEntity<Void> on(InternalException e) {
        this.logger.error("Internal error", e);
        return ResponseEntity.internalServerError().build();
    }

    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<Void> on(NullPointerException e) {
        this.logger.error("Null pointer", e);
        return ResponseEntity.internalServerError().build();
    }

    @ExceptionHandler(BulldozerRuntimeException.class)
    public ResponseEntity<Void> on(BulldozerRuntimeException e) {
        this.logger.error("Unhandled Bulldozer runtime exception", e);
        return ResponseEntity.internalServerError().build();
    }

    @ExceptionHandler(BulldozerException.class)
    public ResponseEntity<Void> on(BulldozerException e) {
        this.logger.error("Unhandled Bulldozer exception", e);
        return ResponseEntity.internalServerError().build();
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Void> on(Exception e) {
        this.logger.error("Unhandled exception", e);
        return ResponseEntity.internalServerError().build();
    }
}
