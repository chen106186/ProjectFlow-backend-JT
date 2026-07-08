package com.jitong.projectflow.common.error;

import com.jitong.projectflow.common.api.ApiResponse;
import com.jitong.projectflow.common.web.TraceIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex, HttpServletRequest request) {
        ErrorCode errorCode = ex.errorCode();
        log.warn("业务异常 traceId={} path={} code={} message={}",
                traceId(), requestPath(request), errorCode.code(), ex.getMessage());
        return ResponseEntity.status(toStatus(errorCode))
                .body(ApiResponse.failure(errorCode.code(), ex.getMessage(), traceId()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                   HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(this::fieldErrorMessage)
                .collect(Collectors.joining("；"));
        if (message.isBlank()) {
            message = ErrorCode.BAD_REQUEST.message();
        }
        log.warn("参数校验失败 traceId={} path={} message={}", traceId(), requestPath(request), message);
        return ResponseEntity.badRequest()
                .body(ApiResponse.failure(ErrorCode.BAD_REQUEST.code(), message, traceId()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException ex,
                                                                HttpServletRequest request) {
        String message = ex.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining("；"));
        if (message.isBlank()) {
            message = ErrorCode.BAD_REQUEST.message();
        }
        log.warn("参数约束失败 traceId={} path={} message={}", traceId(), requestPath(request), message);
        return ResponseEntity.badRequest()
                .body(ApiResponse.failure(ErrorCode.BAD_REQUEST.code(), message, traceId()));
    }

    @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
    ResponseEntity<ApiResponse<Void>> handleAccessDenied(Exception ex, HttpServletRequest request) {
        log.warn("权限异常 traceId={} path={} message={}", traceId(), requestPath(request), ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.failure(ErrorCode.FORBIDDEN.code(), ErrorCode.FORBIDDEN.message(), traceId()));
    }

    @ExceptionHandler(DataAccessException.class)
    ResponseEntity<ApiResponse<Void>> handleDataAccess(DataAccessException ex, HttpServletRequest request) {
        log.error("数据库异常 traceId={} path={}", traceId(), requestPath(request), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure(ErrorCode.INTERNAL_ERROR.code(),
                        "数据保存或查询失败，请检查提交内容后重试。", traceId()));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiResponse<Void>> handleOther(Exception ex, HttpServletRequest request) {
        log.error("系统异常 traceId={} path={}", traceId(), requestPath(request), ex);
        return ResponseEntity.internalServerError()
                .body(ApiResponse.failure(ErrorCode.INTERNAL_ERROR.code(),
                        "系统处理失败，请联系管理员并提供追踪ID：" + traceId(), traceId()));
    }

    private String fieldErrorMessage(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }

    private HttpStatus toStatus(ErrorCode errorCode) {
        return HttpStatus.valueOf(errorCode.code());
    }

    private String traceId() {
        String traceId = MDC.get(TraceIdFilter.TRACE_ID);
        return traceId == null ? "" : traceId;
    }

    private String requestPath(HttpServletRequest request) {
        return request == null ? "" : request.getRequestURI();
    }
}
