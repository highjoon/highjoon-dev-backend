package com.highjoondev.api.category.controller;

import com.highjoondev.api.category.dto.CategoryCreateRequest;
import com.highjoondev.api.category.dto.CategoryResponse;
import com.highjoondev.api.category.dto.CategoryUpdateRequest;
import com.highjoondev.api.category.service.CategoryService;
import com.highjoondev.api.global.response.ApiResponse;
import com.highjoondev.api.global.response.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Category", description = "카테고리 조회/생성 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "카테고리 생성", description = "새로운 카테고리를 생성합니다. 부모 카테고리를 지정하면 하위 카테고리가 생성됩니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "201",
                description = "생성 성공",
                content = @Content(schema = @Schema(implementation = CategoryResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효성 검사 실패")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<CategoryResponse>> create(@Valid @RequestBody CategoryCreateRequest request) {
        CategoryResponse response = categoryService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @Operation(summary = "카테고리 목록 조회", description = "카테고리 목록을 조회합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "조회 성공",
                content = @Content(schema = @Schema(implementation = CategoryResponse.class)))
    })
    @GetMapping
    public ApiResponse<List<CategoryResponse>> findAll() {
        return ApiResponse.ok(categoryService.findAll());
    }

    @Operation(summary = "카테고리 단건 조회", description = "ID로 카테고리를 조회합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "조회 성공",
                content = @Content(schema = @Schema(implementation = CategoryResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "카테고리를 찾을 수 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 ID")
    })
    @GetMapping("/{id}")
    public ApiResponse<CategoryResponse> findById(
            @Parameter(description = "카테고리 ID", required = true) @PathVariable UUID id) {
        return ApiResponse.ok(categoryService.findById(id));
    }

    @Operation(summary = "카테고리 업데이트", description = "카테고리를 업데이트합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "업데이트 성공",
                content = @Content(schema = @Schema(implementation = CategoryResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "카테고리를 찾을 수 없음",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "유효성 검사 실패, 부모 카테고리를 찾을 수 없음, 또는 자기 자신을 부모로 지정함",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> update(
            @Parameter(description = "카테고리 ID", required = true) @PathVariable UUID id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "업데이트 본문") @Valid @RequestBody
                    CategoryUpdateRequest request) {
        CategoryResponse response = categoryService.updateById(id, request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
