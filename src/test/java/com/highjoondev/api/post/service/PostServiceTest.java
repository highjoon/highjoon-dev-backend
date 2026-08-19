package com.highjoondev.api.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.highjoondev.api.category.entity.Category;
import com.highjoondev.api.category.exception.CategoryNotFoundException;
import com.highjoondev.api.category.exception.CategoryReferenceNotFoundException;
import com.highjoondev.api.category.repository.CategoryRepository;
import com.highjoondev.api.post.dto.PostCreateRequest;
import com.highjoondev.api.post.dto.PostDetailResponse;
import com.highjoondev.api.post.dto.PostResponse;
import com.highjoondev.api.post.dto.PostSummary;
import com.highjoondev.api.post.dto.PostUpdateRequest;
import com.highjoondev.api.post.entity.Post;
import com.highjoondev.api.post.exception.DuplicatedFeaturedPostException;
import com.highjoondev.api.post.exception.DuplicatedPostSlugException;
import com.highjoondev.api.post.exception.FeaturedPostCannotBeHiddenException;
import com.highjoondev.api.post.exception.FeaturedPostNotFoundException;
import com.highjoondev.api.post.exception.PostNotFoundException;
import com.highjoondev.api.post.repository.PostRepository;
import com.highjoondev.api.tag.entity.Tag;
import com.highjoondev.api.tag.exception.TagReferenceNotFoundException;
import com.highjoondev.api.tag.repository.TagRepository;
import java.time.Instant;
import java.util.List;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class PostServiceTest {

    private static final Instant PUBLISHED_AT = Instant.parse("2026-05-11T00:00:00Z");
    private static final Instant NEW_PUBLISHED_AT = Instant.parse("2026-06-22T00:00:00Z");

    @Mock
    private PostRepository postRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TagRepository tagRepository;

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
                null,
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
                null,
                isFeatured,
                isHidden);
    }

    /** 태그를 지정한 생성 요청 */
    private PostCreateRequest requestWithTags(List<UUID> tagIds) {
        return new PostCreateRequest(
                "제목",
                "slug",
                "설명",
                "https://example.com/content.md",
                "https://example.com/banner.png",
                PUBLISHED_AT,
                null,
                tagIds,
                false,
                false);
    }

    /** 태그를 지정한 수정 요청 */
    private PostUpdateRequest updateRequestWithTags(List<UUID> tagIds) {
        return new PostUpdateRequest(
                "새 제목",
                "new-slug",
                "새 설명",
                "https://example.com/new-content.md",
                "https://example.com/new-banner.png",
                NEW_PUBLISHED_AT,
                null,
                tagIds,
                false,
                false);
    }

    /** 태그. id를 심어야 서비스가 조회해온 것과 요청 id를 맞춰볼 수 있음 */
    private Tag tagWithId(UUID id, String name) {
        Tag tag = Tag.builder().name(name).build();
        ReflectionTestUtils.setField(tag, "id", id);
        return tag;
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
    @DisplayName("추천이면서 숨김으로 생성 시 예외")
    void create_withFeaturedAndHidden_shouldThrowException() {
        // Given
        var request = request(null, true, true);

        // When, Then
        assertThatThrownBy(() -> postService.create(request)).isInstanceOf(FeaturedPostCannotBeHiddenException.class);
        verify(postRepository, never()).saveAndFlush(any(Post.class));
        // 요청만 보고 막으므로 DB 조회 없음
        verify(postRepository, never()).existsBySlug(any());
        verify(postRepository, never()).findFirstByIsFeaturedTrue();
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
    @DisplayName("추천이 아니면 생성 시 추천 중복 검사 건너뜀")
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
        // Given: 추천과 숨김은 함께 못 켜므로 숨김은 끔
        var request = request(null, true, false);

        // When
        postService.create(request);

        // Then
        ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
        verify(postRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().isFeatured()).isTrue();
        assertThat(captor.getValue().isHidden()).isFalse();
    }

    @Test
    @DisplayName("숨김 글로 생성 시 엔티티에 숨김 반영")
    void create_withHidden_shouldSaveAsHidden() {
        // Given
        var request = request(null, false, true);

        // When
        postService.create(request);

        // Then
        ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
        verify(postRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().isHidden()).isTrue();
        assertThat(captor.getValue().isFeatured()).isFalse();
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
    @DisplayName("카테고리 지정 시 생성 응답에 카테고리 포함")
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
    @DisplayName("없는 카테고리 지정 시 생성 예외")
    void create_withNonExistentCategoryId_shouldThrowException() {
        // Given
        UUID categoryId = UUID.randomUUID();
        var request = request(categoryId, false, false);
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        // When, Then
        assertThatThrownBy(() -> postService.create(request)).isInstanceOf(CategoryReferenceNotFoundException.class);
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
    @DisplayName("태그 지정 시 생성 글에 태그 연결")
    void create_withTagIds_shouldLinkTags() {
        // Given
        UUID springId = UUID.randomUUID();
        UUID jpaId = UUID.randomUUID();
        var request = requestWithTags(List.of(springId, jpaId));
        when(tagRepository.findAllById(anySet()))
                .thenReturn(List.of(tagWithId(springId, "spring"), tagWithId(jpaId, "jpa")));

        // When
        postService.create(request);

        // Then
        ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
        verify(postRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getPostTags())
                .extracting(postTag -> postTag.getTag().getName())
                .containsExactlyInAnyOrder("spring", "jpa");
    }

    @Test
    @DisplayName("없는 태그 지정 시 생성 예외")
    void create_withUnknownTagId_shouldThrowException() {
        // Given: 하나만 찾히고 하나는 없음
        UUID springId = UUID.randomUUID();
        UUID unknownId = UUID.randomUUID();
        var request = requestWithTags(List.of(springId, unknownId));
        when(tagRepository.findAllById(anySet())).thenReturn(List.of(tagWithId(springId, "spring")));

        // When, Then
        assertThatThrownBy(() -> postService.create(request))
                .isInstanceOf(TagReferenceNotFoundException.class)
                .hasMessageContaining(unknownId.toString());
        verify(postRepository, never()).saveAndFlush(any(Post.class));
    }

    @Test
    @DisplayName("같은 태그를 여러 번 지정해도 한 번만 연결")
    void create_withDuplicateTagIds_shouldLinkOnce() {
        // Given: 중복을 안 걸러내면 요청 건수와 조회 건수가 어긋나 없는 태그로 오인함
        UUID springId = UUID.randomUUID();
        var request = requestWithTags(List.of(springId, springId));
        when(tagRepository.findAllById(anySet())).thenReturn(List.of(tagWithId(springId, "spring")));

        // When
        postService.create(request);

        // Then
        ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
        verify(postRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getPostTags()).hasSize(1);
    }

    @Test
    @DisplayName("태그 미지정 시 태그 조회 건너뜀")
    void create_withNullTagIds_shouldSkipTagLookup() {
        // Given
        var request = request(null, false, false);

        // When
        postService.create(request);

        // Then
        verify(tagRepository, never()).findAllById(any());
    }

    @Test
    @DisplayName("정상 요청 시 모든 필드 수정")
    void updateById_withValidRequest_shouldUpdateAllFields() {
        // Given
        UUID id = UUID.randomUUID();
        Post post = existingPost(id);
        // 추천과 숨김은 함께 못 켜므로 추천만 켬
        var request = updateRequest("new-slug", null, true, false);
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
        // 추천, 숨김은 공개 응답에 없으므로 엔티티로 확인
        assertThat(post.isFeatured()).isTrue();
        assertThat(post.isHidden()).isFalse();
    }

    @Test
    @DisplayName("숨김으로 수정 시 엔티티에 숨김 반영")
    void updateById_withHidden_shouldUpdateIsHidden() {
        // Given
        UUID id = UUID.randomUUID();
        Post post = existingPost(id);
        var request = updateRequest("new-slug", null, false, true);
        when(postRepository.findById(id)).thenReturn(Optional.of(post));

        // When
        postService.updateById(id, request);

        // Then
        assertThat(post.isHidden()).isTrue();
        assertThat(post.isFeatured()).isFalse();
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
    @DisplayName("추천이면서 숨김으로 수정 시 예외")
    void updateById_withFeaturedAndHidden_shouldThrowException() {
        // Given: 추천이 아니던 글이라 기존 검사로는 빠져나가던 경우
        UUID id = UUID.randomUUID();
        var request = updateRequest("new-slug", null, true, true);
        when(postRepository.findById(id)).thenReturn(Optional.of(existingPost(id)));

        // When, Then
        assertThatThrownBy(() -> postService.updateById(id, request))
                .isInstanceOf(FeaturedPostCannotBeHiddenException.class);
        verify(postRepository, never()).flush();
    }

    @Test
    @DisplayName("이미 추천인 글을 숨김으로 수정 시 예외")
    void updateById_whenFeaturedTurnsHidden_shouldThrowException() {
        // Given
        UUID id = UUID.randomUUID();
        Post post = existingPost(id);
        post.updateIsFeatured(true);
        var request = updateRequest("new-slug", null, true, true);
        when(postRepository.findById(id)).thenReturn(Optional.of(post));

        // When, Then
        assertThatThrownBy(() -> postService.updateById(id, request))
                .isInstanceOf(FeaturedPostCannotBeHiddenException.class);
        assertThat(post.isHidden()).isFalse();
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
        postService.updateById(id, request);

        // Then
        assertThat(post.isFeatured()).isTrue();
    }

    @Test
    @DisplayName("추천이 아니면 수정 시 추천 중복 검사 건너뜀")
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
    @DisplayName("카테고리 지정 시 수정 응답에 카테고리 포함")
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
    @DisplayName("없는 카테고리 지정 시 수정 예외")
    void updateById_withNonExistentCategoryId_shouldThrowException() {
        // Given
        UUID id = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        var request = updateRequest("new-slug", categoryId, false, false);
        when(postRepository.findById(id)).thenReturn(Optional.of(existingPost(id)));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        // When, Then
        assertThatThrownBy(() -> postService.updateById(id, request))
                .isInstanceOf(CategoryReferenceNotFoundException.class);
    }

    @Test
    @DisplayName("태그 지정 시 기존 태그를 새 태그로 교체")
    void updateById_withTagIds_shouldReplaceTags() {
        // Given
        UUID id = UUID.randomUUID();
        UUID dockerId = UUID.randomUUID();
        Post post = existingPost(id);
        post.updateTags(List.of(tagWithId(UUID.randomUUID(), "spring")));
        var request = updateRequestWithTags(List.of(dockerId));
        when(postRepository.findById(id)).thenReturn(Optional.of(post));
        when(tagRepository.findAllById(anySet())).thenReturn(List.of(tagWithId(dockerId, "docker")));

        // When
        postService.updateById(id, request);

        // Then
        assertThat(post.getPostTags())
                .extracting(postTag -> postTag.getTag().getName())
                .containsExactly("docker");
    }

    @Test
    @DisplayName("태그 미지정으로 수정 시 기존 태그 전부 해제")
    void updateById_withNullTagIds_shouldClearTags() {
        // Given: 카테고리와 같이 값을 안 주면 해제로 봄
        UUID id = UUID.randomUUID();
        Post post = existingPost(id);
        post.updateTags(List.of(tagWithId(UUID.randomUUID(), "spring")));
        var request = updateRequestWithTags(null);
        when(postRepository.findById(id)).thenReturn(Optional.of(post));

        // When
        postService.updateById(id, request);

        // Then
        assertThat(post.getPostTags()).isEmpty();
        verify(tagRepository, never()).findAllById(any());
    }

    @Test
    @DisplayName("없는 태그 지정 시 수정 예외")
    void updateById_withUnknownTagId_shouldThrowException() {
        // Given
        UUID id = UUID.randomUUID();
        UUID unknownId = UUID.randomUUID();
        var request = updateRequestWithTags(List.of(unknownId));
        when(postRepository.findById(id)).thenReturn(Optional.of(existingPost(id)));
        when(tagRepository.findAllById(anySet())).thenReturn(List.of());

        // When, Then
        assertThatThrownBy(() -> postService.updateById(id, request))
                .isInstanceOf(TagReferenceNotFoundException.class)
                .hasMessageContaining(unknownId.toString());
        verify(postRepository, never()).flush();
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

    @Test
    @DisplayName("있는 id 삭제 시 조회한 엔티티를 삭제")
    void deleteById_withExistingId_shouldDeletePost() {
        // Given
        UUID id = UUID.randomUUID();
        Post post = existingPost(id);
        when(postRepository.findById(id)).thenReturn(Optional.of(post));

        // When
        postService.deleteById(id);

        // Then
        verify(postRepository).delete(post);
    }

    @Test
    @DisplayName("없는 id 삭제 시 예외, 삭제 안 함")
    void deleteById_withNonExistentId_shouldThrowException() {
        // Given
        UUID id = UUID.randomUUID();
        when(postRepository.findById(id)).thenReturn(Optional.empty());

        // When, Then
        assertThatThrownBy(() -> postService.deleteById(id)).isInstanceOf(PostNotFoundException.class);
        verify(postRepository, never()).delete(any(Post.class));
    }

    /** 상세 조회 대상 글. 앞뒤 글과 구분되도록 값을 따로 둠 */
    private Post detailPost(String slug, boolean isHidden) {
        Post post = Post.builder()
                .slug(slug)
                .title("상세 제목")
                .description("상세 설명")
                .contentUrl("https://example.com/detail-content.md")
                .bannerImageUrl("https://example.com/detail-banner.png")
                .publishedAt(PUBLISHED_AT)
                .isHidden(isHidden)
                .build();
        ReflectionTestUtils.setField(post, "id", UUID.randomUUID());
        return post;
    }

    private Post adjacentPost(String slug, String title) {
        return Post.builder()
                .slug(slug)
                .title(title)
                .description("설명")
                .contentUrl("https://example.com/c.md")
                .bannerImageUrl("https://example.com/b.png")
                .publishedAt(NEW_PUBLISHED_AT)
                .build();
    }

    @Test
    @DisplayName("정상 조회 시 앞뒤 글까지 반환")
    void findBySlug_withValidSlug_shouldReturnDetailResponse() {
        // Given
        Post post = detailPost("detail-slug", false);
        when(postRepository.findBySlug("detail-slug")).thenReturn(Optional.of(post));
        when(postRepository.findPreviousPost(PUBLISHED_AT, post.getId()))
                .thenReturn(Optional.of(adjacentPost("prev-slug", "이전 제목")));
        when(postRepository.findNextPost(PUBLISHED_AT, post.getId()))
                .thenReturn(Optional.of(adjacentPost("next-slug", "다음 제목")));

        // When
        PostDetailResponse response = postService.findBySlug("detail-slug");

        // Then
        assertThat(response.post().slug()).isEqualTo("detail-slug");
        assertThat(response.post().title()).isEqualTo("상세 제목");
        assertThat(response.post().description()).isEqualTo("상세 설명");
        assertThat(response.post().contentUrl()).isEqualTo("https://example.com/detail-content.md");
        assertThat(response.previous()).isEqualTo(new PostSummary("prev-slug", "이전 제목"));
        assertThat(response.next()).isEqualTo(new PostSummary("next-slug", "다음 제목"));
    }

    @Test
    @DisplayName("없는 slug 조회 시 예외")
    void findBySlug_withNonExistentSlug_shouldThrowException() {
        // Given
        when(postRepository.findBySlug("unknown")).thenReturn(Optional.empty());

        // When, Then
        assertThatThrownBy(() -> postService.findBySlug("unknown")).isInstanceOf(PostNotFoundException.class);
    }

    @Test
    @DisplayName("숨김 글 조회 시 예외, 앞뒤 글 조회 안 함")
    void findBySlug_withHiddenPost_shouldThrowException() {
        // Given
        Post post = detailPost("hidden-slug", true);
        when(postRepository.findBySlug("hidden-slug")).thenReturn(Optional.of(post));

        // When, Then
        assertThatThrownBy(() -> postService.findBySlug("hidden-slug")).isInstanceOf(PostNotFoundException.class);
        verify(postRepository, never()).findPreviousPost(any(), any());
        verify(postRepository, never()).findNextPost(any(), any());
    }

    @Test
    @DisplayName("첫 글 조회 시 이전 글은 null")
    void findBySlug_withFirstPost_shouldReturnNullPrevious() {
        // Given
        Post post = detailPost("first-slug", false);
        when(postRepository.findBySlug("first-slug")).thenReturn(Optional.of(post));
        when(postRepository.findPreviousPost(PUBLISHED_AT, post.getId())).thenReturn(Optional.empty());
        when(postRepository.findNextPost(PUBLISHED_AT, post.getId()))
                .thenReturn(Optional.of(adjacentPost("next-slug", "다음 제목")));

        // When
        PostDetailResponse response = postService.findBySlug("first-slug");

        // Then
        assertThat(response.previous()).isNull();
        assertThat(response.next().slug()).isEqualTo("next-slug");
    }

    @Test
    @DisplayName("마지막 글 조회 시 다음 글은 null")
    void findBySlug_withLastPost_shouldReturnNullNext() {
        // Given
        Post post = detailPost("last-slug", false);
        when(postRepository.findBySlug("last-slug")).thenReturn(Optional.of(post));
        when(postRepository.findPreviousPost(PUBLISHED_AT, post.getId()))
                .thenReturn(Optional.of(adjacentPost("prev-slug", "이전 제목")));
        when(postRepository.findNextPost(PUBLISHED_AT, post.getId())).thenReturn(Optional.empty());

        // When
        PostDetailResponse response = postService.findBySlug("last-slug");

        // Then
        assertThat(response.previous().slug()).isEqualTo("prev-slug");
        assertThat(response.next()).isNull();
    }

    @Test
    @DisplayName("목록 조회 시 응답 변환과 페이지 정보 유지")
    void findAll_withPosts_shouldReturnMappedPage() {
        // Given
        Pageable pageable = PageRequest.of(1, 2);
        // 전체 5건 중 2건짜리 페이지. 목록 크기와 전체 건수를 다르게 둬서 페이지 정보 전달을 확인함
        Page<Post> page = new PageImpl<>(
                List.of(adjacentPost("first-slug", "첫 제목"), adjacentPost("second-slug", "둘째 제목")), pageable, 5);
        when(postRepository.findByIsHiddenFalseOrderByPublishedAtDescIdDesc(pageable))
                .thenReturn(page);

        // When
        Page<PostResponse> response = postService.findAll(null, null, pageable);

        // Then
        assertThat(response.getContent()).extracting(PostResponse::slug).containsExactly("first-slug", "second-slug");
        assertThat(response.getContent()).extracting(PostResponse::title).containsExactly("첫 제목", "둘째 제목");
        assertThat(response.getTotalElements()).isEqualTo(5);
        assertThat(response.getTotalPages()).isEqualTo(3);
        assertThat(response.getNumber()).isEqualTo(1);
    }

    @Test
    @DisplayName("글이 없으면 빈 목록, 예외 아님")
    void findAll_withNoPosts_shouldReturnEmptyPage() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        when(postRepository.findByIsHiddenFalseOrderByPublishedAtDescIdDesc(pageable))
                .thenReturn(Page.empty(pageable));

        // When
        Page<PostResponse> response = postService.findAll(null, null, pageable);

        // Then
        assertThat(response.getContent()).isEmpty();
        assertThat(response.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("tag만 주면 태그별 조회를 씀")
    void findAll_withTagOnly_shouldUseTagQuery() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        when(postRepository.findByIsHiddenFalseAndPostTags_Tag_NameOrderByPublishedAtDescIdDesc("react", pageable))
                .thenReturn(new PageImpl<>(List.of(adjacentPost("tagged", "태그 글")), pageable, 1));

        // When
        Page<PostResponse> response = postService.findAll(null, "react", pageable);

        // Then
        assertThat(response.getContent()).extracting(PostResponse::slug).containsExactly("tagged");
        verify(postRepository, never()).findByIsHiddenFalseOrderByPublishedAtDescIdDesc(any());
    }

    @Test
    @DisplayName("category와 tag를 함께 주면 조합 조회를 씀")
    void findAll_withCategoryAndTag_shouldUseCombinedQuery() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Category category = categoryWithId("backend", null);
        when(categoryRepository.findBySlug("backend")).thenReturn(Optional.of(category));
        when(postRepository.findByIsHiddenFalseAndCategoryIdInAndPostTags_Tag_NameOrderByPublishedAtDescIdDesc(
                        List.of(category.getId()), "react", pageable))
                .thenReturn(new PageImpl<>(List.of(adjacentPost("both", "둘 다")), pageable, 1));

        // When
        Page<PostResponse> response = postService.findAll("backend", "react", pageable);

        // Then
        assertThat(response.getContent()).extracting(PostResponse::slug).containsExactly("both");
        verify(postRepository, never()).findByIsHiddenFalseAndCategoryIdInOrderByPublishedAtDescIdDesc(any(), any());
        verify(postRepository, never())
                .findByIsHiddenFalseAndPostTags_Tag_NameOrderByPublishedAtDescIdDesc(any(), any());
    }

    @Test
    @DisplayName("조합 조회도 자식 카테고리 id를 함께 넘김")
    void findAll_withCategoryAndTag_shouldIncludeChildCategoryIds() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Category parent = categoryWithId("parent", null);
        Category child = categoryWithId("child", parent);
        when(categoryRepository.findBySlug("parent")).thenReturn(Optional.of(parent));
        when(postRepository.findByIsHiddenFalseAndCategoryIdInAndPostTags_Tag_NameOrderByPublishedAtDescIdDesc(
                        any(), any(), any()))
                .thenReturn(Page.empty(pageable));

        // When
        postService.findAll("parent", "react", pageable);

        // Then
        ArgumentCaptor<List<UUID>> captor = ArgumentCaptor.forClass(List.class);
        verify(postRepository)
                .findByIsHiddenFalseAndCategoryIdInAndPostTags_Tag_NameOrderByPublishedAtDescIdDesc(
                        captor.capture(), eq("react"), eq(pageable));
        assertThat(captor.getValue()).containsExactly(parent.getId(), child.getId());
    }

    @Test
    @DisplayName("없는 카테고리 slug면 태그를 함께 줘도 예외")
    void findAll_withUnknownCategoryAndTag_shouldThrowException() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        when(categoryRepository.findBySlug("unknown")).thenReturn(Optional.empty());

        // When, Then
        assertThatThrownBy(() -> postService.findAll("unknown", "react", pageable))
                .isInstanceOf(CategoryNotFoundException.class);
    }

    /** 카테고리. id를 심어야 리포지토리에 넘어간 id 목록을 확인할 수 있음 */
    private Category categoryWithId(String slug, Category parent) {
        // 빌더가 parent.children에 자기를 넣어줌
        Category category =
                Category.builder().title(slug).slug(slug).parent(parent).build();
        ReflectionTestUtils.setField(category, "id", UUID.randomUUID());
        return category;
    }

    @Test
    @DisplayName("카테고리 조회 시 자식 카테고리 글까지 포함")
    void findByCategorySlug_withChildren_shouldQueryOwnAndChildIds() {
        // Given
        Pageable pageable = PageRequest.of(0, 2);
        Category parent = categoryWithId("frontend", null);
        Category firstChild = categoryWithId("react", parent);
        Category secondChild = categoryWithId("vue", parent);
        when(categoryRepository.findBySlug("frontend")).thenReturn(Optional.of(parent));
        // 전체 5건 중 2건짜리 페이지. 목록 크기와 전체 건수를 다르게 둬서 페이지 정보 전달을 확인함
        when(postRepository.findByIsHiddenFalseAndCategoryIdInOrderByPublishedAtDescIdDesc(anyList(), eq(pageable)))
                .thenReturn(new PageImpl<>(
                        List.of(adjacentPost("first-slug", "첫 제목"), adjacentPost("second-slug", "둘째 제목")),
                        pageable,
                        5));

        // When
        Page<PostResponse> response = postService.findAll("frontend", null, pageable);

        // Then
        ArgumentCaptor<List<UUID>> captor = ArgumentCaptor.captor();
        verify(postRepository)
                .findByIsHiddenFalseAndCategoryIdInOrderByPublishedAtDescIdDesc(captor.capture(), eq(pageable));
        assertThat(captor.getValue())
                .containsExactlyInAnyOrder(parent.getId(), firstChild.getId(), secondChild.getId());
        assertThat(response.getContent()).extracting(PostResponse::slug).containsExactly("first-slug", "second-slug");
        assertThat(response.getTotalElements()).isEqualTo(5);
        assertThat(response.getTotalPages()).isEqualTo(3);
    }

    @Test
    @DisplayName("자식 없는 카테고리 조회 시 자기 카테고리 글만")
    void findByCategorySlug_withoutChildren_shouldQueryOwnIdOnly() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Category category = categoryWithId("backend", null);
        when(categoryRepository.findBySlug("backend")).thenReturn(Optional.of(category));
        when(postRepository.findByIsHiddenFalseAndCategoryIdInOrderByPublishedAtDescIdDesc(anyList(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(adjacentPost("only-slug", "유일 제목")), pageable, 1));

        // When
        postService.findAll("backend", null, pageable);

        // Then
        ArgumentCaptor<List<UUID>> captor = ArgumentCaptor.captor();
        verify(postRepository)
                .findByIsHiddenFalseAndCategoryIdInOrderByPublishedAtDescIdDesc(captor.capture(), eq(pageable));
        assertThat(captor.getValue()).containsExactly(category.getId());
    }

    @Test
    @DisplayName("없는 slug 조회 시 예외, 글 조회 안 함")
    void findByCategorySlug_withNonExistentSlug_shouldThrowException() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        when(categoryRepository.findBySlug("unknown")).thenReturn(Optional.empty());

        // When, Then
        assertThatThrownBy(() -> postService.findAll("unknown", null, pageable))
                .isInstanceOf(CategoryNotFoundException.class);
        verify(postRepository, never())
                .findByIsHiddenFalseAndCategoryIdInOrderByPublishedAtDescIdDesc(anyList(), any(Pageable.class));
    }

    @Test
    @DisplayName("카테고리에 글이 없으면 빈 목록, 예외 아님")
    void findByCategorySlug_withNoPosts_shouldReturnEmptyPage() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Category category = categoryWithId("empty", null);
        when(categoryRepository.findBySlug("empty")).thenReturn(Optional.of(category));
        when(postRepository.findByIsHiddenFalseAndCategoryIdInOrderByPublishedAtDescIdDesc(anyList(), eq(pageable)))
                .thenReturn(Page.empty(pageable));

        // When
        Page<PostResponse> response = postService.findAll("empty", null, pageable);

        // Then
        assertThat(response.getContent()).isEmpty();
        assertThat(response.getTotalElements()).isZero();
    }
}
