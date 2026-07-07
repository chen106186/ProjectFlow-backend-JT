package com.jitong.projectflow.common.error;

import com.jitong.projectflow.common.api.ApiResponse;
import com.jitong.projectflow.common.web.TraceIdFilter;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex) {
        ErrorCode errorCode = ex.errorCode();
        return ResponseEntity.status(toStatus(errorCode))
                .body(ApiResponse.failure(errorCode.code(), ex.getMessage(), traceId()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
    ResponseEntity<ApiResponse<Void>> handleValidation(Exception ex) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.failure(ErrorCode.BAD_REQUEST.code(), ErrorCode.BAD_REQUEST.message(), traceId()));
    }

    @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
    ResponseEntity<ApiResponse<Void>> handleAccessDenied(Exception ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.failure(ErrorCode.FORBIDDEN.code(), ErrorCode.FORBIDDEN.message(), traceId()));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiResponse<Void>> handleOther(Exception ex) {
        return ResponseEntity.internalServerError()
                .body(ApiResponse.failure(ErrorCode.INTERNAL_ERROR.code(), ErrorCode.INTERNAL_ERROR.message(), traceId()));
    }

    private HttpStatus toStatus(ErrorCode errorCode) {
        return HttpStatus.valueOf(errorCode.code());
    }

    private String traceId() {
        String traceId = MDC.get(TraceIdFilter.TRACE_ID);
        return traceId == null ? "" : traceId;
    }
}
