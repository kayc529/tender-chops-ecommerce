package com.kaycheung.product_service.exception.handler;

import com.kaycheung.product_service.dto.ApiErrorResponse;
import com.kaycheung.product_service.entity.ProductCategory;
import com.kaycheung.product_service.exception.code.ApiErrorCode;
import com.kaycheung.product_service.exception.code.CommonErrorCode;
import com.kaycheung.product_service.exception.code.ProductErrorCode;
import com.kaycheung.product_service.utils.ApiErrorResponseUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ApiErrorResponse> handleJwtException(JwtException ex, HttpServletRequest request) {
        log.warn("JwtException: {}", ex.getMessage());
        CommonErrorCode c = CommonErrorCode.COMMON_AUTH_INVALID_TOKEN;
        return ResponseEntity.status(c.status()).body(ApiErrorResponseUtil.buildError(c.status(), c, "invalid_token", request));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthenticationException(AuthenticationException ex, HttpServletRequest request) {
        log.warn("AuthenticationException: {}", ex.getMessage());
        CommonErrorCode c = CommonErrorCode.COMMON_AUTH_UNAUTHENTICATED;
        return ResponseEntity.status(c.status()).body(ApiErrorResponseUtil.buildError(c.status(), c, "authentication_failed", request));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDeniedException(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("AccessDeniedException: {}", ex.getMessage());
        CommonErrorCode c = CommonErrorCode.COMMON_AUTH_FORBIDDEN;
        return ResponseEntity.status(c.status()).body(ApiErrorResponseUtil.buildError(c.status(), c, "access_denied", request));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrityViolationExceptions(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("DataIntegrityViolationException: {}", ex.getMessage());
        CommonErrorCode c = CommonErrorCode.COMMON_STATE_CONFLICT;
        return ResponseEntity.status(c.status()).body(ApiErrorResponseUtil.buildError(c.status(), c, "db_conflict", request));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolationException(ConstraintViolationException ex, HttpServletRequest request) {
        log.warn("ConstraintViolationException: {}", ex.getMessage());
        CommonErrorCode c = CommonErrorCode.COMMON_CONSTRAINT_VIOLATION;
        return ResponseEntity.status(c.status()).body(ApiErrorResponseUtil.buildError(c.status(), c, "constraint_violation", request));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.warn("HttpMessageNotReadableException: {}", ex.getMessage());
        CommonErrorCode c = CommonErrorCode.COMMON_MALFORMED_REQUEST_BODY;
        return ResponseEntity.status(c.status()).body(ApiErrorResponseUtil.buildError(c.status(), c, "malformed_request_body", request));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(err -> err.getField() + " " + err.getDefaultMessage())
                .findFirst()
                .orElse("Validation failed");

        log.warn("MethodArgumentNotValidException: {}", message);
        CommonErrorCode c = CommonErrorCode.COMMON_VALIDATION_FAILED;
        return ResponseEntity.status(c.status()).body(ApiErrorResponseUtil.buildError(c.status(), c, message, request));
    }


    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request
    ) {
        log.warn("IllegalArgumentException: {}", ex.getMessage());
        CommonErrorCode c = CommonErrorCode.COMMON_VALIDATION_FAILED;
        return ResponseEntity.status(c.status()).body(ApiErrorResponseUtil.buildError(c.status(), c, ex.getMessage(), request));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalState(
            IllegalStateException ex,
            HttpServletRequest request
    ) {
        log.warn("IllegalStateException: {}", ex.getMessage());
        CommonErrorCode c = CommonErrorCode.COMMON_STATE_CONFLICT;
        return ResponseEntity.status(c.status()).body(ApiErrorResponseUtil.buildError(c.status(), c, ex.getMessage(), request));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        log.warn("HttpRequestMethodNotSupportedException: {}", ex.getMessage());
        CommonErrorCode c = CommonErrorCode.COMMON_METHOD_NOT_ALLOWED;
        return ResponseEntity.status(c.status()).body(ApiErrorResponseUtil.buildError(c.status(), c, ex.getMessage(), request));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingServletRequestParameter(MissingServletRequestParameterException ex, HttpServletRequest request) {
        log.warn("MissingServletRequestParameterException: {}", ex.getMessage());
        CommonErrorCode c = CommonErrorCode.COMMON_CONSTRAINT_VIOLATION;
        return ResponseEntity.status(c.status()).body(ApiErrorResponseUtil.buildError(c.status(), c, ex.getMessage(), request));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        log.warn("MethodArgumentTypeMismatchException: {}", ex.getMessage());
        ApiErrorCode c;

        if (ex.getRequiredType() == ProductCategory.class && "category".equals(ex.getName())) {
            c = ProductErrorCode.PRODUCT_INVALID_CATEGORY;
        }else{
            c = CommonErrorCode.COMMON_CONSTRAINT_VIOLATION;
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiErrorResponseUtil.buildError(HttpStatus.BAD_REQUEST, c, ex.getMessage(), request));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingRequestHeader(MissingRequestHeaderException ex, HttpServletRequest request) {
        log.warn("MissingRequestHeaderException: {}", ex.getMessage());
        CommonErrorCode c = CommonErrorCode.COMMON_MISSING_HEADER;
        return ResponseEntity.status(c.status()).body(ApiErrorResponseUtil.buildError(c.status(), c, ex.getMessage(), request));
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNoHandlerFound(NoHandlerFoundException ex, HttpServletRequest request) {
        log.warn("NoHandlerFoundException: {}", ex.getMessage());
        CommonErrorCode c = CommonErrorCode.COMMON_ROUTE_NOT_FOUND;
        return ResponseEntity.status(c.status()).body(ApiErrorResponseUtil.buildError(c.status(), c, ex.getMessage(), request));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpectedException(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error: ", ex);
        CommonErrorCode c = CommonErrorCode.COMMON_INTERNAL_ERROR;
        return ResponseEntity.status(c.status()).body(ApiErrorResponseUtil.buildError(c.status(), c, "unexpected_error", request));
    }
}
