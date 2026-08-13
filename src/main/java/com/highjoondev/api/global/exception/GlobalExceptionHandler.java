package com.highjoondev.api.global.exception;

import com.highjoondev.api.global.response.ApiResult;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    // 도메인 예외는 전부 BusinessException을 상속하고, 상태 코드는 ErrorCode가 들고 있음
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResult<Void>> handleBusinessException(BusinessException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        return ResponseEntity.status(errorCode.status())
                .body(ApiResult.error(errorCode.code(), exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResult<Void>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException exception) {
        List<String> details = exception.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .toList();

        return ResponseEntity.status(CommonErrorCode.VALIDATION_FAILED.status())
                .body(ApiResult.error(
                        CommonErrorCode.VALIDATION_FAILED.code(),
                        CommonErrorCode.VALIDATION_FAILED.message(),
                        details));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResult<Void>> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException exception) {
        return ResponseEntity.status(CommonErrorCode.INVALID_PARAMETER.status())
                .body(ApiResult.error(
                        CommonErrorCode.INVALID_PARAMETER.code(),
                        CommonErrorCode.INVALID_PARAMETER.message(exception.getName())));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResult<Void>> handleException(Exception exception, HttpServletRequest request) {
        log.error("처리하지 못한 예외: {} {}", request.getMethod(), request.getRequestURI(), exception);

        return ResponseEntity.status(CommonErrorCode.INTERNAL_ERROR.status())
                .body(ApiResult.error(CommonErrorCode.INTERNAL_ERROR.code(), CommonErrorCode.INTERNAL_ERROR.message()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResult<Void>> handleNoResourceFoundException(NoResourceFoundException exception) {
        return ResponseEntity.status(CommonErrorCode.RESOURCE_NOT_FOUND.status())
                .body(ApiResult.error(
                        CommonErrorCode.RESOURCE_NOT_FOUND.code(), CommonErrorCode.RESOURCE_NOT_FOUND.message()));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResult<Void>> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException exception) {
        return ResponseEntity.status(CommonErrorCode.METHOD_NOT_ALLOWED.status())
                .body(ApiResult.error(
                        CommonErrorCode.METHOD_NOT_ALLOWED.code(), CommonErrorCode.METHOD_NOT_ALLOWED.message()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResult<Void>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException exception) {
        return ResponseEntity.status(CommonErrorCode.MALFORMED_REQUEST.status())
                .body(ApiResult.error(
                        CommonErrorCode.MALFORMED_REQUEST.code(), CommonErrorCode.MALFORMED_REQUEST.message()));
    }
}
