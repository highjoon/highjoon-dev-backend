package com.highjoondev.api.global.exception;

import com.highjoondev.api.category.exception.CategoryInvalidParentException;
import com.highjoondev.api.category.exception.CategoryNotFoundException;
import com.highjoondev.api.category.exception.CategoryParentNotFoundException;
import com.highjoondev.api.category.exception.DuplicatedCategorySlugException;
import com.highjoondev.api.global.response.ApiResult;
import com.highjoondev.api.post.exception.DuplicatedFeaturedPostException;
import com.highjoondev.api.post.exception.DuplicatedPostSlugException;
import com.highjoondev.api.post.exception.PostNotFoundException;
import com.highjoondev.api.tag.exception.DuplicatedTagNameException;
import com.highjoondev.api.tag.exception.TagNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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

    @ExceptionHandler(PostNotFoundException.class)
    public ResponseEntity<ApiResult<Void>> handlePostNotFoundException(PostNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResult.error(exception.getErrorCode().code(), exception.getMessage()));
    }

    @ExceptionHandler(DuplicatedPostSlugException.class)
    public ResponseEntity<ApiResult<Void>> handleDuplicatedPostSlugException(DuplicatedPostSlugException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResult.error(exception.getErrorCode().code(), exception.getMessage()));
    }

    @ExceptionHandler(DuplicatedFeaturedPostException.class)
    public ResponseEntity<ApiResult<Void>> handleDuplicatedFeaturedPostException(
            DuplicatedFeaturedPostException exception) {
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

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResult<Void>> handleNoResourceFoundException(NoResourceFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResult.error(
                        CommonErrorCode.RESOURCE_NOT_FOUND.code(), CommonErrorCode.RESOURCE_NOT_FOUND.message()));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResult<Void>> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException exception) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResult.error(
                        CommonErrorCode.METHOD_NOT_ALLOWED.code(), CommonErrorCode.METHOD_NOT_ALLOWED.message()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResult<Void>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResult.error(
                        CommonErrorCode.MALFORMED_REQUEST.code(), CommonErrorCode.MALFORMED_REQUEST.message()));
    }
}
