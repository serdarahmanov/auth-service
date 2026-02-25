package com.serdarahmanov.music_app_backend.utility.api;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import com.serdarahmanov.music_app_backend.auth.refresh.exceptions.RefreshTokenExpiredException;
import com.serdarahmanov.music_app_backend.auth.refresh.exceptions.RefreshTokenInvalidException;
import com.serdarahmanov.music_app_backend.auth.refresh.exceptions.RefreshTokenOwnershipException;
import com.serdarahmanov.music_app_backend.auth.refresh.exceptions.RefreshTokenReuseDetectedException;
import com.serdarahmanov.music_app_backend.auth.security.LoginRateLimitExceededException;
import com.serdarahmanov.music_app_backend.utility.customExceptions.ResetPasswordTokenNotExistException;
import com.serdarahmanov.music_app_backend.utility.customExceptions.VerificationCodeNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AccountStatusException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.UUID;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getDefaultMessage() == null ? "Validation failed" : err.getDefaultMessage())
                .orElse("Validation failed");

        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleBadRequest(IllegalArgumentException ex, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage(), request);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalState(IllegalStateException ex, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage(), request);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        return error(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", ex.getMessage(), request);
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiErrorResponse> handleDisabled(DisabledException ex, HttpServletRequest request) {
        return error(HttpStatus.FORBIDDEN, "ACCOUNT_DISABLED", ex.getMessage(), request);
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ApiErrorResponse> handleLocked(LockedException ex, HttpServletRequest request) {
        return error(HttpStatus.FORBIDDEN, "ACCOUNT_LOCKED", ex.getMessage(), request);
    }

    @ExceptionHandler(AccountStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleAccountStatus(AccountStatusException ex, HttpServletRequest request) {
        return error(HttpStatus.FORBIDDEN, "ACCOUNT_STATUS_INVALID", ex.getMessage(), request);
    }

    @ExceptionHandler(LoginRateLimitExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleRateLimit(LoginRateLimitExceededException ex, HttpServletRequest request) {
        return error(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED", ex.getMessage(), request);
    }

    @ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthMissing(
            AuthenticationCredentialsNotFoundException ex,
            HttpServletRequest request
    ) {
        return error(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", ex.getMessage(), request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return error(HttpStatus.FORBIDDEN, "FORBIDDEN", "Access denied", request);
    }

    @ExceptionHandler(RefreshTokenExpiredException.class)
    public ResponseEntity<ApiErrorResponse> handleRefreshExpired(
            RefreshTokenExpiredException ex,
            HttpServletRequest request
    ) {
        return error(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_EXPIRED", ex.getMessage(), request);
    }

    @ExceptionHandler(RefreshTokenInvalidException.class)
    public ResponseEntity<ApiErrorResponse> handleRefreshInvalid(
            RefreshTokenInvalidException ex,
            HttpServletRequest request
    ) {
        return error(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_INVALID", ex.getMessage(), request);
    }

    @ExceptionHandler(RefreshTokenReuseDetectedException.class)
    public ResponseEntity<ApiErrorResponse> handleRefreshReuse(
            RefreshTokenReuseDetectedException ex,
            HttpServletRequest request
    ) {
        return error(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_REUSED", ex.getMessage(), request);
    }

    @ExceptionHandler(RefreshTokenOwnershipException.class)
    public ResponseEntity<ApiErrorResponse> handleRefreshOwnership(
            RefreshTokenOwnershipException ex,
            HttpServletRequest request
    ) {
        return error(HttpStatus.FORBIDDEN, "REFRESH_TOKEN_FORBIDDEN", ex.getMessage(), request);
    }

    @ExceptionHandler(VerificationCodeNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleVerificationNotFound(
            VerificationCodeNotFoundException ex,
            HttpServletRequest request
    ) {
        return error(HttpStatus.BAD_REQUEST, "VERIFICATION_CODE_NOT_FOUND", ex.getMessage(), request);
    }

    @ExceptionHandler(ResetPasswordTokenNotExistException.class)
    public ResponseEntity<ApiErrorResponse> handleResetTokenNotFound(
            ResetPasswordTokenNotExistException ex,
            HttpServletRequest request
    ) {
        return error(HttpStatus.BAD_REQUEST, "RESET_PASSWORD_TOKEN_NOT_FOUND", ex.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Unexpected error", request);
    }

    private ResponseEntity<ApiErrorResponse> error(
            HttpStatus status,
            String code,
            String message,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(status)
                .body(new ApiErrorResponse(code, message, resolveTraceId(request)));
    }

    private String resolveTraceId(HttpServletRequest request) {
        Object traceIdAttribute = request.getAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE);
        if (traceIdAttribute instanceof String traceId && StringUtils.hasText(traceId)) {
            return traceId;
        }

        String traceIdHeader = request.getHeader(TraceIdFilter.TRACE_ID_HEADER);
        if (StringUtils.hasText(traceIdHeader)) {
            request.setAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE, traceIdHeader);
            return traceIdHeader;
        }

        String traceIdFromMdc = MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY);
        if (StringUtils.hasText(traceIdFromMdc)) {
            request.setAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE, traceIdFromMdc);
            return traceIdFromMdc;
        }

        String generatedTraceId = UUID.randomUUID().toString();
        request.setAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE, generatedTraceId);
        return generatedTraceId;
    }
}
