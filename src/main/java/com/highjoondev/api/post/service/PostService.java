package com.highjoondev.api.post.service;

import com.highjoondev.api.category.entity.Category;
import com.highjoondev.api.category.exception.CategoryNotFoundException;
import com.highjoondev.api.category.repository.CategoryRepository;
import com.highjoondev.api.post.dto.PostCreateRequest;
import com.highjoondev.api.post.dto.PostDetailResponse;
import com.highjoondev.api.post.dto.PostResponse;
import com.highjoondev.api.post.dto.PostSummary;
import com.highjoondev.api.post.dto.PostUpdateRequest;
import com.highjoondev.api.post.entity.Post;
import com.highjoondev.api.post.exception.DuplicatedFeaturedPostException;
import com.highjoondev.api.post.exception.DuplicatedPostSlugException;
import com.highjoondev.api.post.exception.FeaturedPostNotFoundException;
import com.highjoondev.api.post.exception.PostNotFoundException;
import com.highjoondev.api.post.repository.PostRepository;
import java.util.List;
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

    @Transactional
    public PostResponse create(PostCreateRequest request) {
        if (postRepository.existsBySlug(request.slug())) {
            throw new DuplicatedPostSlugException(request.slug());
        }

        if (request.isFeatured()) {
            postRepository.findFirstByIsFeaturedTrue().ifPresent((featured) -> {
                throw new DuplicatedFeaturedPostException(featured.getId());
            });
        }

        Category category = resolveCategory(request.categoryId());
        // 응답에 담을 생성, 수정 시각은 flush 후에 채워짐
        Post post = postRepository.saveAndFlush(request.toEntity(category));

        return PostResponse.from(post);
    }

    @Transactional
    public PostResponse updateById(UUID id, PostUpdateRequest request) {
        Post post = postRepository.findById(id).orElseThrow(() -> new PostNotFoundException(id));

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
        post.update(
                request.slug(),
                request.title(),
                request.description(),
                request.contentUrl(),
                request.bannerImageUrl(),
                request.publishedAt(),
                category);
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

    public Page<PostResponse> findAll(Pageable pageable) {
        return postRepository
                .findByIsHiddenFalseOrderByPublishedAtDescIdDesc(pageable)
                .map(PostResponse::from);
    }

    public Page<PostResponse> findByCategorySlug(String slug, Pageable pageable) {
        Category category = categoryRepository.findBySlug(slug).orElseThrow(() -> new CategoryNotFoundException(slug));

        List<UUID> categoryIds = Stream.concat(
                        Stream.of(category.getId()),
                        category.getChildren().stream().map(Category::getId))
                .toList();

        return postRepository
                .findByIsHiddenFalseAndCategoryIdInOrderByPublishedAtDescIdDesc(categoryIds, pageable)
                .map(PostResponse::from);
    }

    private Category resolveCategory(UUID categoryId) {
        if (categoryId == null) {
            return null;
        }

        return categoryRepository.findById(categoryId).orElseThrow(() -> new CategoryNotFoundException(categoryId));
    }
}
