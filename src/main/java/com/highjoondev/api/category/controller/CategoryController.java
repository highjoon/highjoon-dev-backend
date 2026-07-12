package com.highjoondev.api.category.controller;

import com.highjoondev.api.category.dto.CategoryCreateRequest;
import com.highjoondev.api.category.dto.CategoryResponse;
import com.highjoondev.api.category.service.CategoryService;
import com.highjoondev.api.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    public ResponseEntity<ApiResponse<CategoryResponse>> create(@Valid @RequestBody CategoryCreateRequest request) {
        CategoryResponse response = categoryService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @GetMapping
    public ApiResponse<List<CategoryResponse>> findAll() {
        return ApiResponse.ok(categoryService.findAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<CategoryResponse> findById(@PathVariable UUID id) {
        return ApiResponse.ok(categoryService.findById(id));
    }
}
