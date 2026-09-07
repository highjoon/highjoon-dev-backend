package com.highjoondev.api.post.service;

import com.highjoondev.api.post.dto.PostViewResponse;
import com.highjoondev.api.post.entity.Post;
import com.highjoondev.api.post.exception.PostNotFoundException;
import com.highjoondev.api.post.repository.PostRepository;
import com.highjoondev.api.post.repository.PostViewStatsRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostViewService {
    private final PostRepository postRepository;
    private final PostViewStatsRepository postViewStatsRepository;

    @Transactional
    public PostViewResponse increaseViewCount(String slug) {
        Post post = postRepository.findBySlugAndIsHiddenFalse(slug).orElseThrow(() -> new PostNotFoundException(slug));
        UUID postId = post.getId();
        LocalDate viewedOn = LocalDate.now(ZoneId.of("Asia/Seoul"));

        postRepository.increaseViewCount(postId);
        postViewStatsRepository.increaseViewCount(UUID.randomUUID(), postId, viewedOn);

        return PostViewResponse.from(postRepository.findViewCountById(postId));
    }
}
