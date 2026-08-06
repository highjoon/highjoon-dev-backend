package com.highjoondev.api.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.highjoondev.api.category.entity.Category;
import com.highjoondev.api.category.exception.CategoryNotFoundException;
import com.highjoondev.api.category.repository.CategoryRepository;
import com.highjoondev.api.post.dto.PostCreateRequest;
import com.highjoondev.api.post.dto.PostResponse;
import com.highjoondev.api.post.entity.Post;
import com.highjoondev.api.post.exception.DuplicatedFeaturedPostException;
import com.highjoondev.api.post.exception.DuplicatedPostSlugException;
import com.highjoondev.api.post.repository.PostRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class PostServiceTest {

    private static final Instant PUBLISHED_AT = Instant.parse("2026-05-11T00:00:00Z");

    @Mock
    private PostRepository postRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private PostService postService;

    @BeforeEach
    void setUp() {
        // 목의 saveAndFlush는 기본값이 null이라 저장한 엔티티를 그대로 돌려주도록 지정
        lenient()
                .when(postRepository.saveAndFlush(any(Post.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private PostCreateRequest request(UUID categoryId, boolean isFeatured, boolean isHidden) {
        return new PostCreateRequest(
                "제목",
                "slug",
                "설명",
                "https://example.com/content.md",
                "https://example.com/banner.png",
                PUBLISHED_AT,
                categoryId,
                isFeatured,
                isHidden);
    }

    @Test
    @DisplayName("정상 요청 시 게시물 생성")
    void create_withValidRequest_shouldReturnPostResponse() {
        // Given
        var request = request(null, false, false);

        // When
        PostResponse response = postService.create(request);

        // Then
        assertThat(response.title()).isEqualTo("제목");
        assertThat(response.slug()).isEqualTo("slug");
        assertThat(response.description()).isEqualTo("설명");
        assertThat(response.contentUrl()).isEqualTo("https://example.com/content.md");
        assertThat(response.bannerImageUrl()).isEqualTo("https://example.com/banner.png");
        assertThat(response.publishedAt()).isEqualTo(PUBLISHED_AT);
        assertThat(response.viewCount()).isZero();
        verify(postRepository).saveAndFlush(any(Post.class));
    }

    @Test
    @DisplayName("중복 slug 생성 시 예외")
    void create_withDuplicateSlug_shouldThrowException() {
        // Given
        var request = request(null, false, false);
        when(postRepository.existsBySlug("slug")).thenReturn(true);

        // When, Then
        assertThatThrownBy(() -> postService.create(request)).isInstanceOf(DuplicatedPostSlugException.class);
        verify(postRepository, never()).saveAndFlush(any(Post.class));
    }

    @Test
    @DisplayName("추천 글이 이미 있는데 추천으로 생성 시 예외")
    void create_withExistingFeaturedPost_shouldThrowException() {
        // Given
        var request = request(null, true, false);
        Post featured = Post.builder().slug("featured").title("추천 글").build();
        UUID featuredId = UUID.randomUUID();
        ReflectionTestUtils.setField(featured, "id", featuredId);
        when(postRepository.findFirstByIsFeaturedTrue()).thenReturn(Optional.of(featured));

        // When, Then
        assertThatThrownBy(() -> postService.create(request))
                .isInstanceOf(DuplicatedFeaturedPostException.class)
                .hasMessageContaining(featuredId.toString());
        verify(postRepository, never()).saveAndFlush(any(Post.class));
    }

    @Test
    @DisplayName("추천이 아니면 추천 중복 검사 건너뜀")
    void create_withoutFeatured_shouldSkipFeaturedCheck() {
        // Given
        var request = request(null, false, false);

        // When
        postService.create(request);

        // Then
        verify(postRepository, never()).findFirstByIsFeaturedTrue();
        verify(postRepository).saveAndFlush(any(Post.class));
    }

    @Test
    @DisplayName("추천 글로 생성 시 엔티티에 추천 반영")
    void create_withFeatured_shouldSaveAsFeatured() {
        // Given
        var request = request(null, true, true);

        // When
        postService.create(request);

        // Then
        ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
        verify(postRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().isFeatured()).isTrue();
        assertThat(captor.getValue().isHidden()).isTrue();
    }

    @Test
    @DisplayName("카테고리 미지정 시 미분류로 생성")
    void create_withNullCategoryId_shouldSaveWithoutCategory() {
        // Given
        var request = request(null, false, false);

        // When
        PostResponse response = postService.create(request);

        // Then
        assertThat(response.category()).isNull();
        verify(categoryRepository, never()).findById(any(UUID.class));
    }

    @Test
    @DisplayName("카테고리 지정 시 응답에 카테고리 포함")
    void create_withCategoryId_shouldSaveWithCategory() {
        // Given
        UUID categoryId = UUID.randomUUID();
        var request = request(categoryId, false, false);
        Category category = Category.builder().title("프론트엔드").slug("frontend").build();
        ReflectionTestUtils.setField(category, "id", categoryId);
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));

        // When
        PostResponse response = postService.create(request);

        // Then
        assertThat(response.category().id()).isEqualTo(categoryId);
        assertThat(response.category().slug()).isEqualTo("frontend");
        assertThat(response.category().parent()).isNull();
    }

    @Test
    @DisplayName("없는 카테고리 지정 시 예외")
    void create_withNonExistentCategoryId_shouldThrowException() {
        // Given
        UUID categoryId = UUID.randomUUID();
        var request = request(categoryId, false, false);
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        // When, Then
        assertThatThrownBy(() -> postService.create(request)).isInstanceOf(CategoryNotFoundException.class);
        verify(postRepository, never()).saveAndFlush(any(Post.class));
    }

    @Test
    @DisplayName("slug 중복과 추천 중복이 겹치면 slug 예외 우선")
    void create_withDuplicateSlugAndExistingFeatured_shouldThrowSlugException() {
        // Given: slug 검사가 추천 검사보다 앞서므로 slug 예외가 나와야 함
        var request = request(null, true, false);
        when(postRepository.existsBySlug("slug")).thenReturn(true);

        // When, Then
        assertThatThrownBy(() -> postService.create(request)).isInstanceOf(DuplicatedPostSlugException.class);
        verify(postRepository, never()).findFirstByIsFeaturedTrue();
    }
}
