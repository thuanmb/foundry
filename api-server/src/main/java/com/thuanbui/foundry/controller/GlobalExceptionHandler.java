package com.thuanbui.foundry.controller;

import com.thuanbui.foundry.security.UnauthorizedException;
import com.thuanbui.foundry.services.AsyncJobService.JobAccessDeniedException;
import com.thuanbui.foundry.services.AsyncJobService.JobNotFoundException;
import com.thuanbui.foundry.services.AttestationFailedException;
import com.thuanbui.foundry.services.AuthService.CooldownNotMetException;
import com.thuanbui.foundry.services.AuthService.OtpExpiredException;
import com.thuanbui.foundry.services.AuthService.OtpInvalidException;
import com.thuanbui.foundry.services.AuthService.OtpMaxAttemptsException;
import com.thuanbui.foundry.services.AuthService.RateLimitExceededException;
import com.thuanbui.foundry.services.FeatureDisabledException;
import com.thuanbui.foundry.services.JwtTokenService;
import com.thuanbui.foundry.services.NoteNotFoundException;
import com.thuanbui.foundry.services.RefreshTokenService;
import com.thuanbui.foundry.services.RefreshTokenService.TokenTheftDetectedException;
import com.thuanbui.foundry.services.TurnstileFailedException;
import com.thuanbui.foundry.services.UserProfileService;
import com.thuanbui.foundry.services.UserProfileService.OtpRequiredException;
import com.thuanbui.foundry.services.UserProfileService.UserNotFoundException;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Maps exceptions to RFC 7807 problem details ({@code application/problem+json}) with localized titles. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final MessageSource messageSource;

    public GlobalExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, Locale locale) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return problem(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "error.validation", detail, locale);
    }

    @ExceptionHandler({ IllegalArgumentException.class, MethodArgumentTypeMismatchException.class })
    public ResponseEntity<ErrorResponse> handleBadArgument(RuntimeException ex, Locale locale) {
        return problem(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "error.validation", ex.getMessage(), locale);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException ex, Locale locale) {
        return problem(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "error.validation", "Malformed request body",
                locale);
    }

    @ExceptionHandler({ RateLimitExceededException.class })
    public ResponseEntity<ErrorResponse> handleRateLimitExceeded(RuntimeException ex, Locale locale) {
        return problem(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED", "error.rate_limited", null, locale);
    }

    @ExceptionHandler(TurnstileFailedException.class)
    public ResponseEntity<ErrorResponse> handleTurnstileFailed(TurnstileFailedException ex, Locale locale) {
        return problem(HttpStatus.FORBIDDEN, "TURNSTILE_FAILED", "error.turnstile_failed", null, locale);
    }

    @ExceptionHandler(AttestationFailedException.class)
    public ResponseEntity<ErrorResponse> handleAttestationFailed(AttestationFailedException ex, Locale locale) {
        return problem(HttpStatus.FORBIDDEN, "ATTESTATION_FAILED", "error.attestation_failed", null, locale);
    }

    @ExceptionHandler(CooldownNotMetException.class)
    public ResponseEntity<ErrorResponse> handleCooldownNotMet(CooldownNotMetException ex, Locale locale) {
        return problem(HttpStatus.TOO_MANY_REQUESTS, "COOLDOWN_NOT_MET", "error.otp.cooldown", null, locale);
    }

    @ExceptionHandler(OtpExpiredException.class)
    public ResponseEntity<ErrorResponse> handleOtpExpired(OtpExpiredException ex, Locale locale) {
        return problem(HttpStatus.GONE, "OTP_EXPIRED", "error.otp.expired", null, locale);
    }

    @ExceptionHandler(OtpMaxAttemptsException.class)
    public ResponseEntity<ErrorResponse> handleOtpMaxAttempts(OtpMaxAttemptsException ex, Locale locale) {
        return problem(HttpStatus.TOO_MANY_REQUESTS, "OTP_MAX_ATTEMPTS", "error.otp.max_attempts", null, locale);
    }

    @ExceptionHandler(OtpInvalidException.class)
    public ResponseEntity<ErrorResponse> handleOtpInvalid(OtpInvalidException ex, Locale locale) {
        return problem(HttpStatus.UNAUTHORIZED, "OTP_INVALID", "error.otp.invalid", null, locale);
    }

    @ExceptionHandler(OtpRequiredException.class)
    public ResponseEntity<ErrorResponse> handleOtpRequired(OtpRequiredException ex, Locale locale) {
        return problem(HttpStatus.BAD_REQUEST, "OTP_REQUIRED", "error.otp.required", null, locale);
    }

    @ExceptionHandler({ JwtTokenService.TokenExpiredException.class,
            RefreshTokenService.TokenExpiredException.class })
    public ResponseEntity<ErrorResponse> handleTokenExpired(RuntimeException ex, Locale locale) {
        return problem(HttpStatus.UNAUTHORIZED, "TOKEN_EXPIRED", "error.token.expired", null, locale);
    }

    @ExceptionHandler({ JwtTokenService.TokenInvalidException.class,
            RefreshTokenService.TokenInvalidException.class })
    public ResponseEntity<ErrorResponse> handleTokenInvalid(RuntimeException ex, Locale locale) {
        return problem(HttpStatus.UNAUTHORIZED, "TOKEN_INVALID", "error.token.invalid", null, locale);
    }

    @ExceptionHandler(TokenTheftDetectedException.class)
    public ResponseEntity<ErrorResponse> handleTokenTheftDetected(TokenTheftDetectedException ex, Locale locale) {
        log.warn("Token theft detected");
        return problem(HttpStatus.UNAUTHORIZED, "TOKEN_THEFT_DETECTED", "error.token.theft", null, locale);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedException ex, Locale locale) {
        return problem(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "error.auth.required", null, locale);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex, Locale locale) {
        return problem(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "error.user.not_found", null, locale);
    }

    @ExceptionHandler({ UserProfileService.AccountDeletedException.class,
            com.thuanbui.foundry.services.AuthService.AccountDeletedException.class })
    public ResponseEntity<ErrorResponse> handleAccountDeleted(RuntimeException ex, Locale locale) {
        return problem(HttpStatus.GONE, "ACCOUNT_DELETED", "error.user.deleted", null, locale);
    }

    @ExceptionHandler(FeatureDisabledException.class)
    public ResponseEntity<ErrorResponse> handleFeatureDisabled(FeatureDisabledException ex, Locale locale) {
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "FEATURE_DISABLED", "error.feature_disabled", null, locale);
    }

    @ExceptionHandler(JobNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleJobNotFound(JobNotFoundException ex, Locale locale) {
        return problem(HttpStatus.NOT_FOUND, "JOB_NOT_FOUND", "error.job.not_found", null, locale);
    }

    @ExceptionHandler(JobAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleJobAccessDenied(JobAccessDeniedException ex, Locale locale) {
        return problem(HttpStatus.FORBIDDEN, "JOB_ACCESS_DENIED", "error.job.access_denied", null, locale);
    }

    @ExceptionHandler(NoteNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoteNotFound(NoteNotFoundException ex, Locale locale) {
        return problem(HttpStatus.NOT_FOUND, "NOTE_NOT_FOUND", "error.note.not_found", null, locale);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex, Locale locale, HttpServletRequest request) {
        // Spring MVC exceptions (404 no handler, 405, 415, ...) carry their own status
        if (ex instanceof org.springframework.web.ErrorResponse springError
                && springError.getStatusCode().is4xxClientError()) {
            HttpStatusCode status = springError.getStatusCode();
            String title = HttpStatus.resolve(status.value()) != null
                    ? HttpStatus.valueOf(status.value()).getReasonPhrase()
                    : "Client error";
            return withProblemType(ResponseEntity.status(status))
                    .body(new ErrorResponse("CLIENT_ERROR", title, status.value(), ex.getMessage()));
        }
        log.error("Unhandled exception", ex);
        ErrorResponse body = new ErrorResponse("INTERNAL_ERROR", message("error.server_error", locale),
                500, null, request.getRequestURI(), java.time.Instant.now());
        return withProblemType(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)).body(body);
    }

    private ResponseEntity<ErrorResponse> problem(HttpStatus status, String type, String messageKey, String detail,
            Locale locale) {
        return withProblemType(ResponseEntity.status(status))
                .body(new ErrorResponse(type, message(messageKey, locale), status.value(), detail));
    }

    private static ResponseEntity.BodyBuilder withProblemType(ResponseEntity.BodyBuilder builder) {
        return builder.contentType(MediaType.APPLICATION_PROBLEM_JSON);
    }

    private String message(String key, Locale locale) {
        return messageSource.getMessage(key, null, key, locale);
    }
}
