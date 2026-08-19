package com.highjoondev.api.post.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.highjoondev.api.TestcontainersConfig;
import com.highjoondev.api.category.entity.Category;
import com.highjoondev.api.category.repository.CategoryRepository;
import com.highjoondev.api.post.entity.Post;
import com.highjoondev.api.tag.entity.Tag;
import com.highjoondev.api.tag.repository.TagRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfig.class)
public class PostRepositoryTest {

    private static final Instant DAY_1 = Instant.parse("2026-05-11T00:00:00Z");
    private static final Instant DAY_2 = Instant.parse("2026-05-12T00:00:00Z");
    private static final Instant DAY_3 = Instant.parse("2026-05-13T00:00:00Z");

    @Autowired
    PostRepository postRepository;

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    TagRepository tagRepository;

    @Autowired
    EntityManager entityManager;

    // V7이 넣은 이관 글이 각 테스트가 저장한 글에 섞이지 않도록 비움. 테스트 종료 시 롤백
    @BeforeEach
    void clearImportedPosts() {
        postRepository.deleteAllInBatch();
    }

    private Post.PostBuilder post(String slug, Instant publishedAt) {
        return Post.builder()
                .slug(slug)
                .title(slug)
                .description("설명")
                .contentUrl("https://example.com/" + slug)
                .bannerImageUrl("https://example.com/" + slug + ".png")
                .publishedAt(publishedAt);
    }

    private Post save(Post.PostBuilder builder) {
        return postRepository.saveAndFlush(builder.build());
    }

    // V4가 넣은 태그와 겹치지 않는 이름만 씀
    private Tag tag(String name) {
        return tagRepository.saveAndFlush(Tag.builder().name(name).build());
    }

    private Post save(Post.PostBuilder builder, Tag... tags) {
        Post post = builder.build();
        post.updateTags(List.of(tags));
        return postRepository.saveAndFlush(post);
    }

    @Test
    @DisplayName("글 저장 시 id, 타임스탬프, 기본값 설정")
    void save_withValidPost_shouldPersistAndBeRetrievable() {
        // Given
        Post newPost = post("slug", DAY_1).build();

        // When
        postRepository.saveAndFlush(newPost);

        // Then
        assertThat(newPost.getId()).isNotNull();
        assertThat(newPost.getCreatedAt()).isNotNull();
        assertThat(newPost.getUpdatedAt()).isNotNull();
        assertThat(newPost.getViewCount()).isZero();
        assertThat(newPost.isFeatured()).isFalse();
        assertThat(newPost.isHidden()).isFalse();
    }

    @Test
    @DisplayName("중복 slug 저장 시 예외")
    void save_withDuplicateSlug_shouldThrowException() {
        // Given
        save(post("slug", DAY_1));
        Post duplicate = post("slug", DAY_2).build();

        // When, Then
        assertThatThrownBy(() -> postRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("slug 존재 여부 확인")
    void existsBySlug_withExistingSlug_shouldReturnTrue() {
        // Given
        save(post("slug", DAY_1));

        // When, Then
        assertThat(postRepository.existsBySlug("slug")).isTrue();
        assertThat(postRepository.existsBySlug("other-slug")).isFalse();
    }

    @Test
    @DisplayName("자기 자신 제외 slug 중복 검사")
    void existsBySlugAndIdNot_withOwnSlug_shouldReturnFalse() {
        // Given
        Post saved = save(post("slug", DAY_1));

        // When, Then
        assertThat(postRepository.existsBySlugAndIdNot("slug", saved.getId())).isFalse();
        assertThat(postRepository.existsBySlugAndIdNot("slug", UUID.randomUUID()))
                .isTrue();
    }

    @Test
    @DisplayName("숨김 글도 slug로 조회")
    void findBySlug_withHiddenPost_shouldStillReturnIt() {
        // Given
        save(post("hidden-slug", DAY_1).isHidden(true));

        // When, Then
        assertThat(postRepository.findBySlug("hidden-slug")).isPresent();
        assertThat(postRepository.findBySlug("absent-slug")).isEmpty();
    }

    @Test
    @DisplayName("추천 글 조회 시 숨김 제외, 최신 1건")
    void findFeatured_shouldReturnLatestVisibleFeaturedPost() {
        // Given
        save(post("featured-old", DAY_1).isFeatured(true));
        save(post("featured-new", DAY_2).isFeatured(true));
        save(post("featured-hidden", DAY_3).isFeatured(true).isHidden(true));
        save(post("plain", DAY_3));

        // When
        var found = postRepository.findFirstByIsFeaturedTrueAndIsHiddenFalseOrderByPublishedAtDescIdDesc();

        // Then: 숨김 글 제외 후 남은 것 중 최신
        assertThat(found).get().extracting(Post::getSlug).isEqualTo("featured-new");
    }

    @Test
    @DisplayName("추천 글 발행일 동일 시 목록 첫 글과 같은 1건")
    void findFeatured_withSamePublishedAt_shouldFollowListOrder() {
        // Given: 발행일이 같은 추천 글 다수. id가 랜덤이라 개수를 늘려야 회귀가 드러남
        for (int i = 0; i < 20; i++) {
            save(post("featured-" + i, DAY_2).isFeatured(true));
        }

        // 기대값은 목록 조회에서 가져옴. Java의 UUID 정렬은 Postgres와 순서가 다를 수 있음
        List<Post> listOrder = postRepository
                .findByIsHiddenFalseOrderByPublishedAtDescIdDesc(PageRequest.of(0, 10))
                .getContent();

        // When, Then
        assertThat(postRepository.findFirstByIsFeaturedTrueAndIsHiddenFalseOrderByPublishedAtDescIdDesc())
                .get()
                .extracting(Post::getSlug)
                .isEqualTo(listOrder.get(0).getSlug());
    }

    @Test
    @DisplayName("추천 글 없을 때 빈 결과")
    void findFeatured_withNoFeaturedPost_shouldReturnEmpty() {
        // Given
        save(post("plain", DAY_1));

        // When, Then
        assertThat(postRepository.findFirstByIsFeaturedTrueAndIsHiddenFalseOrderByPublishedAtDescIdDesc())
                .isEmpty();
    }

    @Test
    @DisplayName("목록 조회 시 숨김 제외, 발행일 역순")
    void findAll_shouldExcludeHiddenAndSortByPublishedAtDesc() {
        // Given
        save(post("oldest", DAY_1));
        save(post("newest", DAY_3));
        save(post("middle", DAY_2));
        save(post("hidden", DAY_3).isHidden(true));

        // When
        List<String> slugs = postRepository
                .findByIsHiddenFalseOrderByPublishedAtDescIdDesc(PageRequest.of(0, 10))
                .map(Post::getSlug)
                .getContent();

        // Then
        assertThat(slugs).containsExactly("newest", "middle", "oldest");
    }

    @Test
    @DisplayName("발행일 동일 시 페이지 간 중복이나 누락 없음")
    void findAll_withSamePublishedAt_shouldNotRepeatOrSkipAcrossPages() {
        // Given: 발행일이 모두 같은 글 5개. 순서는 id가 가름
        for (int i = 0; i < 5; i++) {
            save(post("post-" + i, DAY_1));
        }

        // When: 2건씩 세 페이지 조회
        List<String> collected = new java.util.ArrayList<>();
        for (int page = 0; page < 3; page++) {
            collected.addAll(postRepository
                    .findByIsHiddenFalseOrderByPublishedAtDescIdDesc(PageRequest.of(page, 2))
                    .map(Post::getSlug)
                    .getContent());
        }

        // Then: 중복도 누락도 없음
        assertThat(collected)
                .hasSize(5)
                .doesNotHaveDuplicates()
                .containsExactlyInAnyOrder("post-0", "post-1", "post-2", "post-3", "post-4");
    }

    @Test
    @DisplayName("발행일 기준 이전, 다음 글 조회")
    void findAdjacent_shouldReturnNeighborsByPublishedAt() {
        // Given
        save(post("older", DAY_1));
        Post target = save(post("target", DAY_2));
        save(post("newer", DAY_3));

        // When, Then
        assertThat(postRepository.findPreviousPost(target.getPublishedAt(), target.getId()))
                .get()
                .extracting(Post::getSlug)
                .isEqualTo("older");
        assertThat(postRepository.findNextPost(target.getPublishedAt(), target.getId()))
                .get()
                .extracting(Post::getSlug)
                .isEqualTo("newer");
    }

    @Test
    @DisplayName("발행일 동일 시 목록 순서와 같은 앞뒤 이동")
    void findAdjacent_withSamePublishedAt_shouldFollowListOrder() {
        // Given: 발행일이 같은 세 글
        save(post("a", DAY_2));
        save(post("b", DAY_2));
        save(post("c", DAY_2));

        // 기대 순서는 목록 조회에서 가져옴.
        // Java의 UUID.compareTo는 부호 있는 비교라 Postgres의 uuid 정렬과 순서가 다를 수 있음
        List<Post> listOrder = postRepository
                .findByIsHiddenFalseOrderByPublishedAtDescIdDesc(PageRequest.of(0, 10))
                .getContent();
        Post middle = listOrder.get(1);

        // When, Then: 앞뒤 이동은 목록 순서와 일치해야 함
        assertThat(postRepository.findPreviousPost(middle.getPublishedAt(), middle.getId()))
                .get()
                .extracting(Post::getSlug)
                .isEqualTo(listOrder.get(2).getSlug());
        assertThat(postRepository.findNextPost(middle.getPublishedAt(), middle.getId()))
                .get()
                .extracting(Post::getSlug)
                .isEqualTo(listOrder.get(0).getSlug());
    }

    @Test
    @DisplayName("발행일 같은 숨김 글 건너뛰기")
    void findAdjacent_shouldSkipHiddenPostsSharingPublishedAt() {
        // Given: 같은 발행일 숨김 글 다수. id가 랜덤이라 개수를 늘려야 target 앞뒤가 모두 채워짐
        Post target = save(post("target", DAY_2));
        for (int i = 0; i < 20; i++) {
            save(post("hidden-" + i, DAY_2).isHidden(true));
        }
        save(post("older", DAY_1));
        save(post("newer", DAY_3));

        // When, Then: 숨김 글 건너뛰고 다른 날짜 글로 이동
        assertThat(postRepository.findPreviousPost(target.getPublishedAt(), target.getId()))
                .get()
                .extracting(Post::getSlug)
                .isEqualTo("older");
        assertThat(postRepository.findNextPost(target.getPublishedAt(), target.getId()))
                .get()
                .extracting(Post::getSlug)
                .isEqualTo("newer");
    }

    @Test
    @DisplayName("첫 글, 마지막 글에서 빈 결과")
    void findAdjacent_atBothEnds_shouldReturnEmpty() {
        // Given
        Post oldest = save(post("oldest", DAY_1));
        Post newest = save(post("newest", DAY_3));

        // When, Then
        assertThat(postRepository.findPreviousPost(oldest.getPublishedAt(), oldest.getId()))
                .isEmpty();
        assertThat(postRepository.findNextPost(newest.getPublishedAt(), newest.getId()))
                .isEmpty();
    }

    @Test
    @DisplayName("카테고리별 조회 시 숨김, 타 카테고리 제외")
    void findByCategoryIds_shouldReturnOnlyMatchingVisiblePosts() {
        // Given
        Category parent = categoryRepository.saveAndFlush(
                Category.builder().title("부모").slug("parent").build());
        Category child = categoryRepository.saveAndFlush(
                Category.builder().title("자식").slug("child").parent(parent).build());
        Category other = categoryRepository.saveAndFlush(
                Category.builder().title("남").slug("other").build());

        save(post("in-parent", DAY_3).category(parent));
        save(post("in-child", DAY_2).category(child));
        save(post("in-child-hidden", DAY_1).category(child).isHidden(true));
        save(post("in-other", DAY_3).category(other));
        save(post("no-category", DAY_3));

        // When: 부모와 자식 카테고리 함께 조회
        List<String> slugs = postRepository
                .findByIsHiddenFalseAndCategoryIdInOrderByPublishedAtDescIdDesc(
                        List.of(parent.getId(), child.getId()), PageRequest.of(0, 10))
                .map(Post::getSlug)
                .getContent();

        // Then
        assertThat(slugs).containsExactly("in-parent", "in-child");
    }

    @Test
    @DisplayName("글 삭제")
    void delete_withValidPost_shouldBeDeleted() {
        // Given
        Post saved = save(post("slug", DAY_1));

        // When
        postRepository.delete(saved);

        // Then
        assertThat(postRepository.findById(saved.getId())).isEmpty();
    }

    @Test
    @DisplayName("카테고리 삭제 시 글 유지, 카테고리 해제")
    void deleteCategory_shouldKeepPostAndClearItsCategory() {
        // Given: FK가 ON DELETE SET NULL이라 글은 유지
        Category category = categoryRepository.saveAndFlush(
                Category.builder().title("카테고리").slug("category").build());
        UUID postId = save(post("slug", DAY_1).category(category)).getId();

        // 영속성 컨텍스트 비우기.
        // 영속성 컨텍스트에 남아있으면 삭제된 카테고리를 참조하는 글 때문에 flush에서 TransientObjectException 발생
        // (SET NULL은 DB만 아는 동작이라 Hibernate가 모름)
        entityManager.clear();

        // When
        categoryRepository.delete(categoryRepository.findById(category.getId()).orElseThrow());
        categoryRepository.flush();
        entityManager.clear();

        // Then
        Post reloaded = postRepository.findById(postId).orElseThrow();
        assertThat(reloaded.getCategory()).isNull();
    }

    @Test
    @DisplayName("태그별 조회 시 숨김, 다른 태그 제외")
    void findByTagName_shouldReturnOnlyMatchingVisiblePosts() {
        // Given
        Tag spring = tag("spring");
        Tag jpa = tag("jpa");

        save(post("with-spring", DAY_3), spring);
        save(post("with-both", DAY_2), spring, jpa);
        save(post("with-spring-hidden", DAY_1).isHidden(true), spring);
        save(post("with-jpa-only", DAY_3), jpa);
        save(post("no-tag", DAY_3));

        // When
        List<String> slugs = postRepository
                .findByIsHiddenFalseAndPostTags_Tag_NameOrderByPublishedAtDescIdDesc("spring", PageRequest.of(0, 10))
                .map(Post::getSlug)
                .getContent();

        // Then
        assertThat(slugs).containsExactly("with-spring", "with-both");
    }

    @Test
    @DisplayName("태그가 여럿인 글도 결과에 한 번만")
    void findByTagName_withMultiTaggedPost_shouldNotDuplicate() {
        // Given: 조인으로 행이 불어나면 같은 글이 두 번 나옴
        Tag spring = tag("spring");
        Tag jpa = tag("jpa");
        Tag docker = tag("docker");
        save(post("many-tags", DAY_1), spring, jpa, docker);

        // When
        Page<Post> page = postRepository.findByIsHiddenFalseAndPostTags_Tag_NameOrderByPublishedAtDescIdDesc(
                "spring", PageRequest.of(0, 10));

        // Then
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent()).extracting(Post::getSlug).containsExactly("many-tags");
    }

    @Test
    @DisplayName("없는 태그 이름 조회 시 빈 결과")
    void findByTagName_withUnknownTag_shouldReturnEmpty() {
        // Given
        save(post("with-spring", DAY_1), tag("spring"));

        // When, Then
        assertThat(postRepository
                        .findByIsHiddenFalseAndPostTags_Tag_NameOrderByPublishedAtDescIdDesc(
                                "없는태그", PageRequest.of(0, 10))
                        .getContent())
                .isEmpty();
    }

    @Test
    @DisplayName("카테고리와 태그를 함께 주면 둘 다 만족하는 글만")
    void findByCategoryIdsAndTagName_shouldRequireBothConditions() {
        // Given
        // V2가 넣은 카테고리와 겹치지 않는 slug만 씀
        Category backend = categoryRepository.saveAndFlush(
                Category.builder().title("백엔드").slug("cat-backend").build());
        Category frontend = categoryRepository.saveAndFlush(
                Category.builder().title("프론트").slug("cat-frontend").build());
        Tag spring = tag("spring");
        Tag jpa = tag("jpa");

        save(post("backend-spring", DAY_3).category(backend), spring);
        save(post("backend-jpa", DAY_2).category(backend), jpa);
        save(post("frontend-spring", DAY_3).category(frontend), spring);
        save(post("backend-spring-hidden", DAY_1).category(backend).isHidden(true), spring);

        // When
        List<String> slugs = postRepository
                .findByIsHiddenFalseAndCategoryIdInAndPostTags_Tag_NameOrderByPublishedAtDescIdDesc(
                        List.of(backend.getId()), "spring", PageRequest.of(0, 10))
                .map(Post::getSlug)
                .getContent();

        // Then
        assertThat(slugs).containsExactly("backend-spring");
    }

    @Test
    @DisplayName("상세 조회 시 태그까지 함께 가져옴")
    void findBySlug_shouldLoadTags() {
        // Given
        save(post("with-tags", DAY_1), tag("spring"), tag("jpa"));
        entityManager.clear();

        // When
        Post found = postRepository.findBySlug("with-tags").orElseThrow();

        // Then
        assertThat(found.getPostTags())
                .extracting(postTag -> postTag.getTag().getName())
                .containsExactlyInAnyOrder("spring", "jpa");
    }

    @Test
    @DisplayName("태그 교체 시 뺀 것은 지우고 넣은 것은 추가")
    void updateTags_shouldReplaceLinks() {
        // Given
        Tag spring = tag("spring");
        Tag jpa = tag("jpa");
        Post post = save(post("with-tags", DAY_1), spring, jpa);

        // When: 둘 다 빼고 새 태그만 남김
        Tag docker = tag("docker");
        post.updateTags(List.of(docker));
        postRepository.flush();
        entityManager.clear();

        // Then
        assertThat(postRepository.findById(post.getId()).orElseThrow().getPostTags())
                .extracting(postTag -> postTag.getTag().getName())
                .containsExactly("docker");
    }

    @Test
    @DisplayName("겹치는 태그로 교체 시 유니크 제약 위반 없음")
    void updateTags_withOverlappingTag_shouldNotViolateUniqueConstraint() {
        // Given: 같은 flush 안에서 spring 연결이 지워졌다 다시 추가됨
        Tag spring = tag("spring");
        Tag jpa = tag("jpa");
        Post post = save(post("with-tags", DAY_1), spring, jpa);

        // When: spring은 유지, jpa는 빼고 docker를 넣음
        Tag docker = tag("docker");
        post.updateTags(List.of(spring, docker));
        postRepository.flush();
        entityManager.clear();

        // Then
        assertThat(postRepository.findById(post.getId()).orElseThrow().getPostTags())
                .extracting(postTag -> postTag.getTag().getName())
                .containsExactlyInAnyOrder("spring", "docker");
    }

    @Test
    @DisplayName("빈 목록으로 교체 시 태그 연결 전부 해제")
    void updateTags_withEmptyList_shouldRemoveAllLinks() {
        // Given
        Post post = save(post("with-tags", DAY_1), tag("spring"), tag("jpa"));

        // When
        post.updateTags(List.of());
        postRepository.flush();
        entityManager.clear();

        // Then
        assertThat(postRepository.findById(post.getId()).orElseThrow().getPostTags())
                .isEmpty();
    }

    @Test
    @DisplayName("글 삭제 시 태그 연결도 삭제, 태그는 유지")
    void deletePost_shouldRemoveLinksButKeepTags() {
        // Given
        Tag spring = tag("spring");
        Post post = save(post("with-tags", DAY_1), spring);
        entityManager.clear();

        // When
        postRepository.delete(postRepository.findById(post.getId()).orElseThrow());
        postRepository.flush();
        entityManager.clear();

        // Then
        assertThat(tagRepository.findById(spring.getId())).isPresent();
    }
}
