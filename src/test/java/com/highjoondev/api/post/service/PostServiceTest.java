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
import com.highjoondev.api.post.dto.PostUpdateRequest;
import com.highjoondev.api.post.entity.Post;
import com.highjoondev.api.post.exception.DuplicatedFeaturedPostException;
import com.highjoondev.api.post.exception.DuplicatedPostSlugException;
import com.highjoondev.api.post.exception.FeaturedPostNotFoundException;
import com.highjoondev.api.post.exception.PostNotFoundException;
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
    private static final Instant NEW_PUBLISHED_AT = Instant.parse("2026-06-22T00:00:00Z");

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

    /** 수정 요청. 필드마다 값을 다르게 둬서 인자 순서가 바뀌면 잡히게 함 */
    private PostUpdateRequest updateRequest(String slug, UUID categoryId, boolean isFeatured, boolean isHidden) {
        return new PostUpdateRequest(
                "새 제목",
                slug,
                "새 설명",
                "https://example.com/new-content.md",
                "https://example.com/new-banner.png",
                NEW_PUBLISHED_AT,
                categoryId,
                isFeatured,
                isHidden);
    }

    /** 수정 대상 기존 글. 값이 요청과 전부 달라야 수정 여부를 확인할 수 있음 */
    private Post existingPost(UUID id) {
        Post post = Post.builder()
                .slug("old-slug")
                .title("옛 제목")
                .description("옛 설명")
                .contentUrl("https://example.com/old-content.md")
                .bannerImageUrl("https://example.com/old-banner.png")
                .publishedAt(PUBLISHED_AT)
                .build();
        ReflectionTestUtils.setField(post, "id", id);
        return post;
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

    @Test
    @DisplayName("정상 요청 시 모든 필드 수정")
    void updateById_withValidRequest_shouldUpdateAllFields() {
        // Given
        UUID id = UUID.randomUUID();
        Post post = existingPost(id);
        var request = updateRequest("new-slug", null, true, true);
        when(postRepository.findById(id)).thenReturn(Optional.of(post));

        // When
        PostResponse response = postService.updateById(id, request);

        // Then
        assertThat(response.title()).isEqualTo("새 제목");
        assertThat(response.slug()).isEqualTo("new-slug");
        assertThat(response.description()).isEqualTo("새 설명");
        assertThat(response.contentUrl()).isEqualTo("https://example.com/new-content.md");
        assertThat(response.bannerImageUrl()).isEqualTo("https://example.com/new-banner.png");
        assertThat(response.publishedAt()).isEqualTo(NEW_PUBLISHED_AT);
        assertThat(response.isFeatured()).isTrue();
        assertThat(response.isHidden()).isTrue();
    }

    @Test
    @DisplayName("없는 id 수정 시 예외")
    void updateById_withNonExistentId_shouldThrowException() {
        // Given
        UUID id = UUID.randomUUID();
        var request = updateRequest("new-slug", null, false, false);
        when(postRepository.findById(id)).thenReturn(Optional.empty());

        // When, Then
        assertThatThrownBy(() -> postService.updateById(id, request)).isInstanceOf(PostNotFoundException.class);
    }

    @Test
    @DisplayName("다른 글이 쓰는 slug으로 수정 시 예외")
    void updateById_withDuplicateSlug_shouldThrowException() {
        // Given
        UUID id = UUID.randomUUID();
        var request = updateRequest("taken-slug", null, false, false);
        when(postRepository.findById(id)).thenReturn(Optional.of(existingPost(id)));
        when(postRepository.existsBySlugAndIdNot("taken-slug", id)).thenReturn(true);

        // When, Then
        assertThatThrownBy(() -> postService.updateById(id, request)).isInstanceOf(DuplicatedPostSlugException.class);
    }

    @Test
    @DisplayName("자기 slug 유지한 채 수정 시 통과")
    void updateById_withOwnSlug_shouldNotThrow() {
        // Given: 자기 자신을 뺀 검사만 통과해야 하므로, 뺀 검사는 false 안 뺀 검사는 true로 둠
        UUID id = UUID.randomUUID();
        var request = updateRequest("old-slug", null, false, false);
        when(postRepository.findById(id)).thenReturn(Optional.of(existingPost(id)));
        lenient().when(postRepository.existsBySlug("old-slug")).thenReturn(true);
        when(postRepository.existsBySlugAndIdNot("old-slug", id)).thenReturn(false);

        // When
        PostResponse response = postService.updateById(id, request);

        // Then
        assertThat(response.slug()).isEqualTo("old-slug");
        assertThat(response.title()).isEqualTo("새 제목");
    }

    @Test
    @DisplayName("다른 추천 글이 있는데 추천으로 수정 시 예외")
    void updateById_withExistingFeaturedPost_shouldThrowException() {
        // Given
        UUID id = UUID.randomUUID();
        UUID featuredId = UUID.randomUUID();
        var request = updateRequest("new-slug", null, true, false);
        when(postRepository.findById(id)).thenReturn(Optional.of(existingPost(id)));
        when(postRepository.findFirstByIsFeaturedTrueAndIdNot(id)).thenReturn(Optional.of(existingPost(featuredId)));

        // When, Then
        assertThatThrownBy(() -> postService.updateById(id, request))
                .isInstanceOf(DuplicatedFeaturedPostException.class)
                .hasMessageContaining(featuredId.toString());
    }

    @Test
    @DisplayName("이미 추천인 글을 추천 유지한 채 수정 시 통과")
    void updateById_whenAlreadyFeatured_shouldNotThrow() {
        // Given: 자기 자신을 뺀 검사만 통과해야 하므로, 뺀 검사는 비우고 안 뺀 검사는 채움
        UUID id = UUID.randomUUID();
        Post post = existingPost(id);
        var request = updateRequest("new-slug", null, true, false);
        when(postRepository.findById(id)).thenReturn(Optional.of(post));
        lenient().when(postRepository.findFirstByIsFeaturedTrue()).thenReturn(Optional.of(post));
        when(postRepository.findFirstByIsFeaturedTrueAndIdNot(id)).thenReturn(Optional.empty());

        // When
        PostResponse response = postService.updateById(id, request);

        // Then
        assertThat(response.isFeatured()).isTrue();
    }

    @Test
    @DisplayName("추천이 아니면 추천 중복 검사 건너뜀")
    void updateById_withoutFeatured_shouldSkipFeaturedCheck() {
        // Given
        UUID id = UUID.randomUUID();
        var request = updateRequest("new-slug", null, false, false);
        when(postRepository.findById(id)).thenReturn(Optional.of(existingPost(id)));

        // When
        postService.updateById(id, request);

        // Then
        verify(postRepository, never()).findFirstByIsFeaturedTrueAndIdNot(any(UUID.class));
    }

    @Test
    @DisplayName("카테고리 지정 시 응답에 카테고리 포함")
    void updateById_withCategoryId_shouldUpdateCategory() {
        // Given
        UUID id = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        var request = updateRequest("new-slug", categoryId, false, false);
        Category category = Category.builder().title("프론트엔드").slug("frontend").build();
        ReflectionTestUtils.setField(category, "id", categoryId);
        when(postRepository.findById(id)).thenReturn(Optional.of(existingPost(id)));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));

        // When
        PostResponse response = postService.updateById(id, request);

        // Then
        assertThat(response.category().id()).isEqualTo(categoryId);
        assertThat(response.category().slug()).isEqualTo("frontend");
    }

    @Test
    @DisplayName("카테고리 미지정 시 미분류로 수정")
    void updateById_withNullCategoryId_shouldRemoveCategory() {
        // Given
        UUID id = UUID.randomUUID();
        Post post = existingPost(id);
        ReflectionTestUtils.setField(
                post,
                "category",
                Category.builder().title("프론트엔드").slug("frontend").build());
        var request = updateRequest("new-slug", null, false, false);
        when(postRepository.findById(id)).thenReturn(Optional.of(post));

        // When
        PostResponse response = postService.updateById(id, request);

        // Then
        assertThat(response.category()).isNull();
        verify(categoryRepository, never()).findById(any(UUID.class));
    }

    @Test
    @DisplayName("없는 카테고리 지정 시 예외")
    void updateById_withNonExistentCategoryId_shouldThrowException() {
        // Given
        UUID id = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        var request = updateRequest("new-slug", categoryId, false, false);
        when(postRepository.findById(id)).thenReturn(Optional.of(existingPost(id)));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        // When, Then
        assertThatThrownBy(() -> postService.updateById(id, request)).isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    @DisplayName("추천 글 조회 시 응답 반환")
    void findFeatured_withFeaturedPost_shouldReturnPostResponse() {
        // Given
        UUID id = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        Category category = Category.builder().title("프론트엔드").slug("frontend").build();
        ReflectionTestUtils.setField(category, "id", categoryId);

        Post post = Post.builder()
                .slug("featured-slug")
                .title("추천 제목")
                .description("추천 설명")
                .contentUrl("https://example.com/featured-content.md")
                .bannerImageUrl("https://example.com/featured-banner.png")
                .publishedAt(PUBLISHED_AT)
                .isFeatured(true)
                .category(category)
                .build();
        ReflectionTestUtils.setField(post, "id", id);
        when(postRepository.findFirstByIsFeaturedTrueAndIsHiddenFalseOrderByPublishedAtDescIdDesc())
                .thenReturn(Optional.of(post));

        // When
        PostResponse response = postService.findFeatured();

        // Then
        assertThat(response.id()).isEqualTo(id);
        assertThat(response.slug()).isEqualTo("featured-slug");
        assertThat(response.title()).isEqualTo("추천 제목");
        assertThat(response.description()).isEqualTo("추천 설명");
        assertThat(response.contentUrl()).isEqualTo("https://example.com/featured-content.md");
        assertThat(response.bannerImageUrl()).isEqualTo("https://example.com/featured-banner.png");
        assertThat(response.publishedAt()).isEqualTo(PUBLISHED_AT);
        assertThat(response.isFeatured()).isTrue();
        assertThat(response.category().slug()).isEqualTo("frontend");
    }

    @Test
    @DisplayName("추천 글 없으면 예외")
    void findFeatured_withNoFeaturedPost_shouldThrowException() {
        // Given
        when(postRepository.findFirstByIsFeaturedTrueAndIsHiddenFalseOrderByPublishedAtDescIdDesc())
                .thenReturn(Optional.empty());

        // When, Then
        assertThatThrownBy(() -> postService.findFeatured()).isInstanceOf(FeaturedPostNotFoundException.class);
    }
}
