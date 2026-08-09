package com.highjoondev.api.post.service;

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
import java.util.UUID;
import lombok.RequiredArgsConstructor;
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

    private Category resolveCategory(UUID categoryId) {
        if (categoryId == null) {
            return null;
        }

        return categoryRepository.findById(categoryId).orElseThrow(() -> new CategoryNotFoundException(categoryId));
    }
}
