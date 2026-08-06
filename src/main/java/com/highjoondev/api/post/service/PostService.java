package com.highjoondev.api.post.service;

import com.highjoondev.api.category.entity.Category;
import com.highjoondev.api.category.exception.CategoryNotFoundException;
import com.highjoondev.api.category.repository.CategoryRepository;
import com.highjoondev.api.post.dto.PostCreateRequest;
import com.highjoondev.api.post.dto.PostResponse;
import com.highjoondev.api.post.entity.Post;
import com.highjoondev.api.post.exception.DuplicatedFeaturedPostException;
import com.highjoondev.api.post.exception.DuplicatedPostSlugException;
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
        Post post = request.toEntity(category);
        postRepository.save(post);
        return PostResponse.from(post);
    }

    private Category resolveCategory(UUID categoryId) {
        if (categoryId == null) {
            return null;
        }

        return categoryRepository.findById(categoryId).orElseThrow(() -> new CategoryNotFoundException(categoryId));
    }
}
