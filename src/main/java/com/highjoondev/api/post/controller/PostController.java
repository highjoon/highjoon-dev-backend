package com.highjoondev.api.post.controller;

import com.highjoondev.api.global.response.ApiResult;
import com.highjoondev.api.post.dto.PostCreateRequest;
import com.highjoondev.api.post.dto.PostDetailResponse;
import com.highjoondev.api.post.dto.PostResponse;
import com.highjoondev.api.post.dto.PostUpdateRequest;
import com.highjoondev.api.post.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Tag(name = "Post", description = "게시물 조회/생성/수정/삭제 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/posts")
public class PostController {

    private final PostService postService;

    @Operation(summary = "게시물 생성", description = "새로운 게시물을 생성합니다. categoryId를 넘기지 않으면 미분류로 저장됩니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "생성 성공"),
        @ApiResponse(
                responseCode = "400",
                description = "유효성 검사 실패",
                content = @Content(schema = @Schema(implementation = ApiResult.class))),
        @ApiResponse(
                responseCode = "404",
                description = "카테고리를 찾을 수 없음",
                content = @Content(schema = @Schema(implementation = ApiResult.class))),
        @ApiResponse(
                responseCode = "409",
                description = "중복된 slug 또는 이미 추천 게시물이 있음",
                content = @Content(schema = @Schema(implementation = ApiResult.class)))
    })
    @PostMapping
    public ResponseEntity<ApiResult<PostResponse>> create(@Valid @RequestBody PostCreateRequest request) {
        PostResponse response = postService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{slug}")
                .buildAndExpand(response.slug())
                .toUri();
        return ResponseEntity.created(location).body(ApiResult.ok(response));
    }

    @Operation(summary = "게시물 수정", description = "전 필드를 받아 게시물을 수정합니다. categoryId를 넘기지 않으면 미분류로 저장됩니다.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "수정 성공",
                content = @Content(schema = @Schema(implementation = PostResponse.class))),
        @ApiResponse(
                responseCode = "400",
                description = "유효성 검사 실패 또는 추천 게시물을 숨기려 함",
                content = @Content(schema = @Schema(implementation = ApiResult.class))),
        @ApiResponse(
                responseCode = "404",
                description = "게시물이나 카테고리를 찾을 수 없음",
                content = @Content(schema = @Schema(implementation = ApiResult.class))),
        @ApiResponse(
                responseCode = "409",
                description = "중복된 slug 또는 이미 추천 게시물이 있음",
                content = @Content(schema = @Schema(implementation = ApiResult.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<ApiResult<PostResponse>> update(
            @Parameter(description = "게시물 ID", required = true) @PathVariable UUID id,
            @Valid @RequestBody PostUpdateRequest request) {
        PostResponse response = postService.updateById(id, request);
        return ResponseEntity.ok(ApiResult.ok(response));
    }

    @Operation(summary = "게시물 제거", description = "게시물을 제거합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "제거 성공"),
        @ApiResponse(
                responseCode = "404",
                description = "게시물을 찾을 수 없음",
                content = @Content(schema = @Schema(implementation = ApiResult.class))),
        @ApiResponse(responseCode = "400", description = "잘못된 ID")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResult<Void>> delete(
            @Parameter(description = "게시물 ID", required = true) @PathVariable UUID id) {
        postService.deleteById(id);
        return ResponseEntity.ok(ApiResult.ok(null));
    }

    @Operation(
            summary = "게시물 목록 조회",
            description = "숨김이 아닌 게시물을 최신순으로 조회합니다. category를 넘기면 해당 카테고리와 하위 카테고리의 게시물만 조회합니다. "
                    + "정렬은 발행일 최신순으로 고정이라 sort 파라미터는 반영되지 않습니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(
                responseCode = "404",
                description = "카테고리를 찾을 수 없음",
                content = @Content(schema = @Schema(implementation = ApiResult.class)))
    })
    @GetMapping
    public ResponseEntity<ApiResult<PagedModel<PostResponse>>> findAll(
            @Parameter(description = "카테고리 slug") @RequestParam(required = false) String category,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        Page<PostResponse> posts =
                (category == null) ? postService.findAll(pageable) : postService.findByCategorySlug(category, pageable);
        return ResponseEntity.ok(ApiResult.ok(new PagedModel<>(posts)));
    }

    @Operation(summary = "추천 게시물 조회", description = "숨김이 아닌 추천 게시물을 조회합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(
                responseCode = "404",
                description = "추천 게시물이 존재하지 않음",
                content = @Content(schema = @Schema(implementation = ApiResult.class)))
    })
    @GetMapping("/featured")
    public ResponseEntity<ApiResult<PostResponse>> findFeatured() {
        return ResponseEntity.ok(ApiResult.ok(postService.findFeatured()));
    }

    @Operation(summary = "게시물 상세 조회", description = "slug로 게시물을 조회합니다. 이전 글과 다음 글 정보가 함께 담깁니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(
                responseCode = "404",
                description = "게시물을 찾을 수 없거나 숨김 상태",
                content = @Content(schema = @Schema(implementation = ApiResult.class)))
    })
    @GetMapping("/{slug}")
    public ResponseEntity<ApiResult<PostDetailResponse>> findBySlug(
            @Parameter(description = "게시물 slug", required = true) @PathVariable String slug) {
        return ResponseEntity.ok(ApiResult.ok(postService.findBySlug(slug)));
    }
}
