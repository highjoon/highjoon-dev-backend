package com.highjoondev.api.tag.controller;

import com.highjoondev.api.global.response.ApiResult;
import com.highjoondev.api.tag.dto.TagCreateRequest;
import com.highjoondev.api.tag.dto.TagResponse;
import com.highjoondev.api.tag.dto.TagUpdateRequest;
import com.highjoondev.api.tag.service.TagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Tag(name = "Tag", description = "태그 조회/생성/수정/삭제 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/tags")
public class TagController {

    private final TagService tagService;

    @Operation(summary = "태그 생성", description = "새로운 태그를 생성합니다.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "201",
                description = "생성 성공",
                content = @Content(schema = @Schema(implementation = TagResponse.class))),
        @ApiResponse(responseCode = "400", description = "유효성 검사 실패"),
        @ApiResponse(
                responseCode = "409",
                description = "중복된 이름",
                content = @Content(schema = @Schema(implementation = ApiResult.class)))
    })
    @PostMapping
    public ResponseEntity<ApiResult<TagResponse>> create(@Valid @RequestBody TagCreateRequest request) {
        TagResponse response = tagService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(ApiResult.ok(response));
    }

    @Operation(summary = "태그 목록 조회", description = "태그 목록을 조회합니다.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "조회 성공",
                content = @Content(schema = @Schema(implementation = TagResponse.class)))
    })
    @GetMapping
    public ResponseEntity<ApiResult<List<TagResponse>>> findAll() {
        return ResponseEntity.ok(ApiResult.ok(tagService.findAll()));
    }

    @Operation(summary = "태그 단건 조회", description = "ID로 태그를 조회합니다.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "조회 성공",
                content = @Content(schema = @Schema(implementation = TagResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "태그를 찾을 수 없음",
                content = @Content(schema = @Schema(implementation = ApiResult.class))),
        @ApiResponse(responseCode = "400", description = "잘못된 ID")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResult<TagResponse>> findById(
            @Parameter(description = "태그 ID", required = true) @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResult.ok(tagService.findById(id)));
    }

    @Operation(summary = "태그 이름으로 조회", description = "이름으로 태그를 조회합니다.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "조회 성공",
                content = @Content(schema = @Schema(implementation = TagResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "태그를 찾을 수 없음",
                content = @Content(schema = @Schema(implementation = ApiResult.class)))
    })
    @GetMapping("/name/{name}")
    public ResponseEntity<ApiResult<TagResponse>> findByName(
            @Parameter(description = "태그 이름", required = true) @PathVariable String name) {
        return ResponseEntity.ok(ApiResult.ok(tagService.findByName(name)));
    }

    @Operation(summary = "태그 업데이트", description = "태그를 업데이트합니다.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "업데이트 성공",
                content = @Content(schema = @Schema(implementation = TagResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "태그를 찾을 수 없음",
                content = @Content(schema = @Schema(implementation = ApiResult.class))),
        @ApiResponse(responseCode = "400", description = "유효성 검사 실패"),
        @ApiResponse(
                responseCode = "409",
                description = "중복된 이름",
                content = @Content(schema = @Schema(implementation = ApiResult.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<ApiResult<TagResponse>> update(
            @Parameter(description = "태그 ID", required = true) @PathVariable UUID id,
            @Valid @RequestBody TagUpdateRequest request) {
        TagResponse response = tagService.updateById(id, request);
        return ResponseEntity.ok(ApiResult.ok(response));
    }

    @Operation(summary = "태그 제거", description = "태그를 제거합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "제거 성공"),
        @ApiResponse(
                responseCode = "404",
                description = "태그를 찾을 수 없음",
                content = @Content(schema = @Schema(implementation = ApiResult.class))),
        @ApiResponse(responseCode = "400", description = "잘못된 ID")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResult<Void>> deleteById(
            @Parameter(description = "태그 ID", required = true) @PathVariable UUID id) {
        tagService.deleteById(id);
        return ResponseEntity.ok(ApiResult.ok(null));
    }
}
