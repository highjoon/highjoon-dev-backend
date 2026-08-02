package com.highjoondev.api.global.exception;

import com.highjoondev.api.category.exception.CategoryInvalidParentException;
import com.highjoondev.api.category.exception.CategoryNotFoundException;
import com.highjoondev.api.category.exception.CategoryParentNotFoundException;
import com.highjoondev.api.category.exception.DuplicatedCategorySlugException;
import com.highjoondev.api.global.response.ApiResult;
import com.highjoondev.api.tag.exception.DuplicatedTagNameException;
import com.highjoondev.api.tag.exception.TagNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(CategoryNotFoundException.class)
    public ResponseEntity<ApiResult<Void>> handleCategoryNotFoundException(CategoryNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResult.error(exception.getErrorCode().code(), exception.getMessage()));
    }

    @ExceptionHandler(CategoryParentNotFoundException.class)
    public ResponseEntity<ApiResult<Void>> handleCategoryParentNotFoundException(
            CategoryParentNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResult.error(exception.getErrorCode().code(), exception.getMessage()));
    }

    @ExceptionHandler(CategoryInvalidParentException.class)
    public ResponseEntity<ApiResult<Void>> handleCategoryInvalidParentException(
            CategoryInvalidParentException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResult.error(exception.getErrorCode().code(), exception.getMessage()));
    }

    @ExceptionHandler(DuplicatedCategorySlugException.class)
    public ResponseEntity<ApiResult<Void>> handleDuplicatedCategorySlugException(
            DuplicatedCategorySlugException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResult.error(exception.getErrorCode().code(), exception.getMessage()));
    }

    @ExceptionHandler(TagNotFoundException.class)
    public ResponseEntity<ApiResult<Void>> handleTagNotFoundException(TagNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResult.error(exception.getErrorCode().code(), exception.getMessage()));
    }

    @ExceptionHandler(DuplicatedTagNameException.class)
    public ResponseEntity<ApiResult<Void>> handleDuplicatedTagNameException(DuplicatedTagNameException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResult.error(exception.getErrorCode().code(), exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResult<Void>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException exception) {
        List<String> details = exception.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .toList();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResult.error(
                        CommonErrorCode.VALIDATION_FAILED.code(),
                        CommonErrorCode.VALIDATION_FAILED.message(),
                        details));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResult<Void>> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResult.error(
                        CommonErrorCode.INVALID_PARAMETER.code(),
                        CommonErrorCode.INVALID_PARAMETER.message(exception.getName())));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResult<Void>> handleException(Exception exception, HttpServletRequest request) {
        log.error("처리하지 못한 예외: {} {}", request.getMethod(), request.getRequestURI(), exception);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResult.error(CommonErrorCode.INTERNAL_ERROR.code(), CommonErrorCode.INTERNAL_ERROR.message()));
    }
}
