package com.highjoondev.api.post.service;

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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {
    private final PostRepository postRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;

    @Transactional
    public PostResponse create(PostCreateRequest request) {
        if (request.isFeatured() && request.isHidden()) {
            throw new FeaturedPostCannotBeHiddenException();
        }

        if (postRepository.existsBySlug(request.slug())) {
            throw new DuplicatedPostSlugException(request.slug());
        }

        if (request.isFeatured()) {
            postRepository.findFirstByIsFeaturedTrue().ifPresent((featured) -> {
                throw new DuplicatedFeaturedPostException(featured.getId());
            });
        }

        Category category = resolveCategory(request.categoryId());
        List<Tag> tags = resolveTags(request.tagIds());
        Post post = request.toEntity(category);
        post.updateTags(tags);
        // 응답에 담을 생성, 수정 시각은 flush 후에 채워짐
        postRepository.saveAndFlush(post);

        return PostResponse.from(post);
    }

    @Transactional
    public PostResponse updateById(UUID id, PostUpdateRequest request) {
        Post post = postRepository.findById(id).orElseThrow(() -> new PostNotFoundException(id));

        // 추천 게시물을 숨길 수는 없음
        if (request.isFeatured() && request.isHidden()) {
            throw new FeaturedPostCannotBeHiddenException();
        }

        // slug가 중복되는 경우
        if (postRepository.existsBySlugAndIdNot(request.slug(), id)) {
            throw new DuplicatedPostSlugException(request.slug());
        }

        // 추천 게시물 설정이면, 중복 추천 여부 확인
        if (request.isFeatured()) {
            postRepository.findFirstByIsFeaturedTrueAndIdNot(id).ifPresent((featured) -> {
                throw new DuplicatedFeaturedPostException(featured.getId());
            });
        }

        Category category = resolveCategory(request.categoryId());
        List<Tag> tags = resolveTags(request.tagIds());
        post.update(
                request.slug(),
                request.title(),
                request.description(),
                request.contentUrl(),
                request.bannerImageUrl(),
                request.publishedAt(),
                category);
        post.updateTags(tags);
        post.updateIsFeatured(request.isFeatured());
        post.updateIsHidden(request.isHidden());
        // 응답에 담을 수정 시각은 flush 후에 갱신됨
        postRepository.flush();

        return PostResponse.from(post);
    }

    public PostResponse findFeatured() {
        Post featuredPost = postRepository
                .findFirstByIsFeaturedTrueAndIsHiddenFalseOrderByPublishedAtDescIdDesc()
                .orElseThrow(FeaturedPostNotFoundException::new);
        return PostResponse.from(featuredPost);
    }

    @Transactional
    public void deleteById(UUID id) {
        Post post = postRepository.findById(id).orElseThrow(() -> new PostNotFoundException(id));
        postRepository.delete(post);
    }

    public PostDetailResponse findBySlug(String slug) {
        Post post = postRepository.findBySlug(slug).orElseThrow(() -> new PostNotFoundException(slug));

        if (post.isHidden()) {
            throw new PostNotFoundException(slug);
        }

        PostSummary previous = postRepository
                .findPreviousPost(post.getPublishedAt(), post.getId())
                .map(PostSummary::from)
                .orElse(null);
        PostSummary next = postRepository
                .findNextPost(post.getPublishedAt(), post.getId())
                .map(PostSummary::from)
                .orElse(null);

        return new PostDetailResponse(PostResponse.from(post), previous, next);
    }

    public Page<PostResponse> findAll(String categorySlug, String tagName, Pageable pageable) {
        return findPosts(categorySlug, tagName, pageable).map(PostResponse::from);
    }

    /** 넘어온 조건에 맞는 조회를 고름 */
    private Page<Post> findPosts(String categorySlug, String tagName, Pageable pageable) {
        if (categorySlug == null && tagName == null) {
            return postRepository.findByIsHiddenFalseOrderByPublishedAtDescIdDesc(pageable);
        }

        /* tagName만 넘어온 경우 */
        if (categorySlug == null) {
            return postRepository.findByIsHiddenFalseAndPostTags_Tag_NameOrderByPublishedAtDescIdDesc(
                    tagName, pageable);
        }

        List<UUID> categoryIds = resolveCategoryIds(categorySlug);

        /* categorySlug만 넘어온 경우 */
        if (tagName == null) {
            return postRepository.findByIsHiddenFalseAndCategoryIdInOrderByPublishedAtDescIdDesc(categoryIds, pageable);
        }

        /* tagName, categorySlug 둘 다 넘어온 경우 */
        return postRepository.findByIsHiddenFalseAndCategoryIdInAndPostTags_Tag_NameOrderByPublishedAtDescIdDesc(
                categoryIds, tagName, pageable);
    }

    /** 카테고리 자신과 자식 id. 손자는 안 봄 */
    private List<UUID> resolveCategoryIds(String categorySlug) {
        Category category = categoryRepository
                .findBySlug(categorySlug)
                .orElseThrow(() -> new CategoryNotFoundException(categorySlug));

        return Stream.concat(
                        Stream.of(category.getId()),
                        category.getChildren().stream().map(Category::getId))
                .toList();
    }

    private Category resolveCategory(UUID categoryId) {
        if (categoryId == null) {
            return null;
        }

        return categoryRepository
                .findById(categoryId)
                .orElseThrow(() -> new CategoryReferenceNotFoundException(categoryId));
    }

    private List<Tag> resolveTags(List<UUID> tagIds) {
        if (tagIds == null) {
            return List.of();
        }

        Set<UUID> deduplicatedTagIds = new HashSet<>(tagIds);
        List<Tag> foundTags = tagRepository.findAllById(deduplicatedTagIds);
        List<UUID> foundTagIds = foundTags.stream().map(Tag::getId).toList();

        if (foundTags.size() != deduplicatedTagIds.size()) {
            List<UUID> notFoundTagIds = deduplicatedTagIds.stream()
                    .filter(id -> !foundTagIds.contains(id))
                    .toList();
            throw new TagReferenceNotFoundException(notFoundTagIds);
        }

        return foundTags;
    }
}
