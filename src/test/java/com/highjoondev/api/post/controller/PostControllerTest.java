package com.highjoondev.api.post.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.highjoondev.api.category.exception.CategoryNotFoundException;
import com.highjoondev.api.post.dto.CategoryRef;
import com.highjoondev.api.post.dto.PostCreateRequest;
import com.highjoondev.api.post.dto.PostDetailResponse;
import com.highjoondev.api.post.dto.PostResponse;
import com.highjoondev.api.post.dto.PostSummary;
import com.highjoondev.api.post.dto.PostUpdateRequest;
import com.highjoondev.api.post.exception.DuplicatedFeaturedPostException;
import com.highjoondev.api.post.exception.DuplicatedPostSlugException;
import com.highjoondev.api.post.exception.FeaturedPostCannotBeHiddenException;
import com.highjoondev.api.post.exception.FeaturedPostNotFoundException;
import com.highjoondev.api.post.exception.PostNotFoundException;
import com.highjoondev.api.post.service.PostService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// 컨트롤러 동작만 검증하도록 쓰기 차단 필터를 끔
@WebMvcTest(value = PostController.class, properties = "app.write-api.enabled=true")
public class PostControllerTest {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    PostService postService;

    private static final Instant PUBLISHED_AT = Instant.parse("2026-01-01T00:00:00Z");

    private PostResponse postResponse(String slug, String title) {
        return new PostResponse(
                UUID.randomUUID(),
                slug,
                title,
                "요약",
                "https://example.com/content.md",
                "https://example.com/banner.png",
                PUBLISHED_AT,
                0,
                new CategoryRef(UUID.randomUUID(), "react", "React", null),
                PUBLISHED_AT,
                PUBLISHED_AT);
    }

    private PostCreateRequest createRequest(String title, String slug) {
        return new PostCreateRequest(
                title,
                slug,
                "요약",
                "https://example.com/content.md",
                "https://example.com/banner.png",
                PUBLISHED_AT,
                null,
                false,
                false);
    }

    private PostUpdateRequest updateRequest(String title, String slug) {
        return new PostUpdateRequest(
                title,
                slug,
                "요약",
                "https://example.com/content.md",
                "https://example.com/banner.png",
                PUBLISHED_AT,
                null,
                false,
                false);
    }

    @Test
    @DisplayName("목록 조회 시 200과 페이지 구조 응답")
    void findAll_shouldReturn200WithPagedContent() throws Exception {
        var posts = List.of(postResponse("first", "첫 글"), postResponse("second", "둘째 글"));
        when(postService.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(posts, PageRequest.of(0, 20), 2));

        mockMvc.perform(get("/api/v1/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.content[0].slug").value("first"))
                .andExpect(jsonPath("$.data.page.size").value(20))
                .andExpect(jsonPath("$.data.page.number").value(0))
                .andExpect(jsonPath("$.data.page.totalElements").value(2))
                .andExpect(jsonPath("$.data.page.totalPages").value(1));
    }

    @Test
    @DisplayName("category 파라미터가 있으면 카테고리 조회로 위임")
    void findAll_withCategory_shouldDelegateToCategoryQuery() throws Exception {
        when(postService.findByCategorySlug(eq("react"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(postResponse("first", "첫 글")), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/posts").param("category", "react"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1));

        verify(postService).findByCategorySlug(eq("react"), any(Pageable.class));
        verify(postService, never()).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("category 파라미터가 없으면 전체 조회로 위임")
    void findAll_withoutCategory_shouldDelegateToFindAll() throws Exception {
        when(postService.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/api/v1/posts")).andExpect(status().isOk());

        verify(postService).findAll(any(Pageable.class));
        verify(postService, never()).findByCategorySlug(any(), any(Pageable.class));
    }

    @Test
    @DisplayName("page와 size 파라미터가 Pageable에 전달")
    void findAll_withPageAndSize_shouldPassPageableToService() throws Exception {
        when(postService.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 5), 0));

        mockMvc.perform(get("/api/v1/posts").param("page", "1").param("size", "5"))
                .andExpect(status().isOk());

        var captor = ArgumentCaptor.forClass(Pageable.class);
        verify(postService).findAll(captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(1);
        assertThat(captor.getValue().getPageSize()).isEqualTo(5);
    }

    @Test
    @DisplayName("페이지 파라미터가 없으면 기본 페이지 크기 20")
    void findAll_withoutPageParams_shouldUseDefaultSize() throws Exception {
        when(postService.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/api/v1/posts")).andExpect(status().isOk());

        var captor = ArgumentCaptor.forClass(Pageable.class);
        verify(postService).findAll(captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(0);
        assertThat(captor.getValue().getPageSize()).isEqualTo(20);
    }

    @Test
    @DisplayName("없는 카테고리 조회 시 404")
    void findAll_withUnknownCategory_shouldReturn404() throws Exception {
        when(postService.findByCategorySlug(eq("unknown"), any(Pageable.class)))
                .thenThrow(new CategoryNotFoundException("unknown"));

        mockMvc.perform(get("/api/v1/posts").param("category", "unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("CATEGORY_NOT_FOUND"));
    }

    @Test
    @DisplayName("추천 게시물 조회 시 200")
    void findFeatured_shouldReturn200() throws Exception {
        var response = postResponse("featured-post", "추천 글");
        when(postService.findFeatured()).thenReturn(response);

        mockMvc.perform(get("/api/v1/posts/featured"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(response.id().toString()))
                .andExpect(jsonPath("$.data.slug").value("featured-post"))
                .andExpect(jsonPath("$.data.title").value("추천 글"))
                .andExpect(jsonPath("$.data.category.slug").value("react"));
    }

    @Test
    @DisplayName("추천 게시물이 없으면 404")
    void findFeatured_whenNotFound_shouldReturn404() throws Exception {
        when(postService.findFeatured()).thenThrow(new FeaturedPostNotFoundException());

        mockMvc.perform(get("/api/v1/posts/featured"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("POST_FEATURED_NOT_FOUND"));
    }

    @Test
    @DisplayName("/featured 경로가 slug 경로로 잡히지 않음")
    void findFeatured_shouldNotMatchSlugMapping() throws Exception {
        when(postService.findFeatured()).thenReturn(postResponse("featured-post", "추천 글"));

        mockMvc.perform(get("/api/v1/posts/featured")).andExpect(status().isOk());

        verify(postService).findFeatured();
        verify(postService, never()).findBySlug(any());
    }

    @Test
    @DisplayName("상세 조회 시 200과 이전, 다음 글 포함")
    void findBySlug_shouldReturn200WithNeighbors() throws Exception {
        var detail = new PostDetailResponse(
                postResponse("current", "현재 글"),
                new PostSummary("prev-post", "이전 글"),
                new PostSummary("next-post", "다음 글"));
        when(postService.findBySlug("current")).thenReturn(detail);

        mockMvc.perform(get("/api/v1/posts/{slug}", "current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.post.slug").value("current"))
                .andExpect(jsonPath("$.data.post.title").value("현재 글"))
                .andExpect(jsonPath("$.data.previous.slug").value("prev-post"))
                .andExpect(jsonPath("$.data.next.slug").value("next-post"));
    }

    @Test
    @DisplayName("이전, 다음 글이 없으면 previous와 next가 null로 직렬화")
    void findBySlug_withoutNeighbors_shouldSerializeNeighborsAsNull() throws Exception {
        var detail = new PostDetailResponse(postResponse("only", "유일한 글"), null, null);
        when(postService.findBySlug("only")).thenReturn(detail);

        // ApiResult의 NON_NULL은 자기 필드에만 걸려서 중첩된 previous, next는 null 그대로 나감
        mockMvc.perform(get("/api/v1/posts/{slug}", "only"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.post.slug").value("only"))
                .andExpect(jsonPath("$.data.previous").hasJsonPath())
                .andExpect(jsonPath("$.data.next").hasJsonPath())
                .andExpect(jsonPath("$.data.previous").value(nullValue()))
                .andExpect(jsonPath("$.data.next").value(nullValue()));
    }

    @Test
    @DisplayName("없는 slug 조회 시 404")
    void findBySlug_whenNotFound_shouldReturn404() throws Exception {
        when(postService.findBySlug("unknown")).thenThrow(new PostNotFoundException("unknown"));

        mockMvc.perform(get("/api/v1/posts/{slug}", "unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("POST_NOT_FOUND"));
    }

    @Test
    @DisplayName("게시물 생성 시 201과 Location 헤더")
    void create_withValidRequest_shouldReturn201() throws Exception {
        var request = createRequest("첫 글", "first");
        var response = postResponse("first", "첫 글");
        when(postService.create(request)).thenReturn(response);

        mockMvc.perform(post("/api/v1/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                // Location은 절대 URL이고, 상세 조회 경로와 같게 slug가 담김
                .andExpect(header().string("Location", "http://localhost/api/v1/posts/" + response.slug()))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.slug").value("first"))
                .andExpect(jsonPath("$.data.title").value("첫 글"));
    }

    @Test
    @DisplayName("제목이 비면 400")
    void create_withBlankTitle_shouldReturn400() throws Exception {
        var request = createRequest("", "first");

        mockMvc.perform(post("/api/v1/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("중복 slug 생성 시 409")
    void create_withDuplicateSlug_shouldReturn409() throws Exception {
        var request = createRequest("첫 글", "first");
        when(postService.create(request)).thenThrow(new DuplicatedPostSlugException("first"));

        mockMvc.perform(post("/api/v1/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("POST_DUPLICATED_SLUG"));
    }

    @Test
    @DisplayName("추천 게시물이 이미 있으면 409")
    void create_withDuplicateFeatured_shouldReturn409() throws Exception {
        var request = createRequest("첫 글", "first");
        when(postService.create(request)).thenThrow(new DuplicatedFeaturedPostException(UUID.randomUUID()));

        mockMvc.perform(post("/api/v1/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("POST_DUPLICATED_FEATURED"));
    }

    @Test
    @DisplayName("없는 카테고리로 생성 시 404")
    void create_withUnknownCategory_shouldReturn404() throws Exception {
        var categoryId = UUID.randomUUID();
        var request = new PostCreateRequest(
                "첫 글",
                "first",
                "요약",
                "https://example.com/content.md",
                "https://example.com/banner.png",
                PUBLISHED_AT,
                categoryId,
                false,
                false);
        when(postService.create(request)).thenThrow(new CategoryNotFoundException(categoryId));

        mockMvc.perform(post("/api/v1/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("CATEGORY_NOT_FOUND"));
    }

    @Test
    @DisplayName("게시물 수정 시 200과 수정 결과")
    void update_withValidRequest_shouldReturn200() throws Exception {
        var id = UUID.randomUUID();
        var request = updateRequest("고친 글", "fixed");
        when(postService.updateById(id, request)).thenReturn(postResponse("fixed", "고친 글"));

        mockMvc.perform(put("/api/v1/posts/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.slug").value("fixed"))
                .andExpect(jsonPath("$.data.title").value("고친 글"));

        verify(postService).updateById(id, request);
    }

    @Test
    @DisplayName("수정 시 제목이 비면 400")
    void update_withBlankTitle_shouldReturn400() throws Exception {
        var request = updateRequest("", "fixed");

        mockMvc.perform(put("/api/v1/posts/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));

        verify(postService, never()).updateById(any(), any());
    }

    @Test
    @DisplayName("UUID 형식이 아닌 id로 수정 시 400")
    void update_withMalformedId_shouldReturn400() throws Exception {
        var request = updateRequest("고친 글", "fixed");

        mockMvc.perform(put("/api/v1/posts/{id}", "not-a-uuid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        verify(postService, never()).updateById(any(), any());
    }

    @Test
    @DisplayName("없는 게시물 수정 시 404")
    void update_whenNotFound_shouldReturn404() throws Exception {
        var id = UUID.randomUUID();
        var request = updateRequest("고친 글", "fixed");
        when(postService.updateById(id, request)).thenThrow(new PostNotFoundException(id));

        mockMvc.perform(put("/api/v1/posts/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("POST_NOT_FOUND"));
    }

    @Test
    @DisplayName("없는 카테고리로 수정 시 404")
    void update_withUnknownCategory_shouldReturn404() throws Exception {
        var id = UUID.randomUUID();
        var categoryId = UUID.randomUUID();
        var request = new PostUpdateRequest(
                "고친 글",
                "fixed",
                "요약",
                "https://example.com/content.md",
                "https://example.com/banner.png",
                PUBLISHED_AT,
                categoryId,
                false,
                false);
        when(postService.updateById(id, request)).thenThrow(new CategoryNotFoundException(categoryId));

        mockMvc.perform(put("/api/v1/posts/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("CATEGORY_NOT_FOUND"));
    }

    @Test
    @DisplayName("중복 slug로 수정 시 409")
    void update_withDuplicateSlug_shouldReturn409() throws Exception {
        var id = UUID.randomUUID();
        var request = updateRequest("고친 글", "fixed");
        when(postService.updateById(id, request)).thenThrow(new DuplicatedPostSlugException("fixed"));

        mockMvc.perform(put("/api/v1/posts/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("POST_DUPLICATED_SLUG"));
    }

    @Test
    @DisplayName("다른 추천 게시물이 이미 있으면 409")
    void update_withDuplicateFeatured_shouldReturn409() throws Exception {
        var id = UUID.randomUUID();
        var request = new PostUpdateRequest(
                "고친 글",
                "fixed",
                "요약",
                "https://example.com/content.md",
                "https://example.com/banner.png",
                PUBLISHED_AT,
                null,
                true,
                false);
        when(postService.updateById(id, request)).thenThrow(new DuplicatedFeaturedPostException(UUID.randomUUID()));

        mockMvc.perform(put("/api/v1/posts/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("POST_DUPLICATED_FEATURED"));
    }

    @Test
    @DisplayName("추천 게시물을 숨기려 하면 400")
    void update_withFeaturedAndHidden_shouldReturn400() throws Exception {
        var id = UUID.randomUUID();
        var request = new PostUpdateRequest(
                "고친 글",
                "fixed",
                "요약",
                "https://example.com/content.md",
                "https://example.com/banner.png",
                PUBLISHED_AT,
                null,
                true,
                true);
        when(postService.updateById(id, request)).thenThrow(new FeaturedPostCannotBeHiddenException());

        mockMvc.perform(put("/api/v1/posts/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("POST_FEATURED_CANNOT_BE_HIDDEN"));
    }

    @Test
    @DisplayName("게시물 제거 시 200과 빈 본문")
    void delete_shouldReturn200() throws Exception {
        var id = UUID.randomUUID();

        // ApiResult의 NON_NULL 때문에 data 키 자체가 빠짐
        mockMvc.perform(delete("/api/v1/posts/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(postService).deleteById(id);
    }

    @Test
    @DisplayName("없는 게시물 제거 시 404")
    void delete_whenNotFound_shouldReturn404() throws Exception {
        var id = UUID.randomUUID();
        doThrow(new PostNotFoundException(id)).when(postService).deleteById(id);

        mockMvc.perform(delete("/api/v1/posts/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("POST_NOT_FOUND"));
    }

    @Test
    @DisplayName("UUID 형식이 아닌 id로 제거 시 400")
    void delete_withMalformedId_shouldReturn400() throws Exception {
        mockMvc.perform(delete("/api/v1/posts/{id}", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        verify(postService, never()).deleteById(any());
    }
}
