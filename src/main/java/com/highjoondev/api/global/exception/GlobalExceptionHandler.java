package com.highjoondev.api.global.exception;

import com.highjoondev.api.category.exception.CategoryInvalidParentException;
import com.highjoondev.api.category.exception.CategoryNotFoundException;
import com.highjoondev.api.category.exception.CategoryParentNotFoundException;
import com.highjoondev.api.category.exception.DuplicatedCategorySlugException;
import com.highjoondev.api.global.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(CategoryNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleCategoryNotFoundException(CategoryNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(exception.getErrorCode().code(), exception.getMessage()));
    }

    @ExceptionHandler(CategoryParentNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleCategoryParentNotFoundException(
            CategoryParentNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(exception.getErrorCode().code(), exception.getMessage()));
    }

    @ExceptionHandler(CategoryInvalidParentException.class)
    public ResponseEntity<ApiResponse<Void>> handleCategoryInvalidParentException(
            CategoryInvalidParentException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(exception.getErrorCode().code(), exception.getMessage()));
    }

    @ExceptionHandler(DuplicatedCategorySlugException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicatedCategorySlugException(
            DuplicatedCategorySlugException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(exception.getErrorCode().code(), exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException exception) {
        List<String> details = exception.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .toList();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(
                        ErrorCode.VALIDATION_FAILED.code(), ErrorCode.VALIDATION_FAILED.message(), details));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(
                        ErrorCode.INVALID_PARAMETER.code(), ErrorCode.INVALID_PARAMETER.message(exception.getName())));
    }
}
