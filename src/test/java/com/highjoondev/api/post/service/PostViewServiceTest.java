package com.highjoondev.api.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.highjoondev.api.post.dto.PostViewResponse;
import com.highjoondev.api.post.entity.Post;
import com.highjoondev.api.post.exception.PostNotFoundException;
import com.highjoondev.api.post.repository.PostRepository;
import com.highjoondev.api.post.repository.PostViewStatsRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class PostViewServiceTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final String SLUG = "hello";

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostViewStatsRepository postViewStatsRepository;

    @InjectMocks
    private PostViewService postViewService;

    /** 엔티티의 조회수와 재조회 결과를 다르게 둬야 어느 값을 응답에 담았는지 드러남 */
    private Post post(int staleViewCount) {
        Post post = Post.builder()
                .slug(SLUG)
                .title("제목")
                .description("설명")
                .contentUrl("https://example.com/hello")
                .bannerImageUrl("https://example.com/hello.png")
                .publishedAt(Instant.parse("2026-05-11T00:00:00Z"))
                .build();
        ReflectionTestUtils.setField(post, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(post, "viewCount", staleViewCount);
        return post;
    }

    @Test
    @DisplayName("조회수 반영 시 게시물과 일별 집계가 함께 증가")
    void increaseViewCount_shouldUpdateBoth() {
        Post post = post(10);
        when(postRepository.findBySlugAndIsHiddenFalse(SLUG)).thenReturn(Optional.of(post));
        when(postRepository.findViewCountById(post.getId())).thenReturn(11);

        postViewService.increaseViewCount(SLUG);

        verify(postRepository).increaseViewCount(post.getId());
        verify(postViewStatsRepository).increaseViewCount(any(), eq(post.getId()), any());
    }

    @Test
    @DisplayName("응답에는 다시 읽은 조회수가 담김")
    void increaseViewCount_shouldReturnRefetchedCount() {
        // 엔티티는 갱신 전 값(10)을 들고 있음. 벌크 UPDATE라 메모리 값이 안 바뀜
        Post post = post(10);
        when(postRepository.findBySlugAndIsHiddenFalse(SLUG)).thenReturn(Optional.of(post));
        when(postRepository.findViewCountById(post.getId())).thenReturn(11);

        PostViewResponse response = postViewService.increaseViewCount(SLUG);

        assertThat(response.viewCount()).isEqualTo(11);
    }

    @Test
    @DisplayName("일별 집계는 KST 오늘 날짜로 반영")
    void increaseViewCount_shouldUseKstToday() {
        Post post = post(0);
        when(postRepository.findBySlugAndIsHiddenFalse(SLUG)).thenReturn(Optional.of(post));
        when(postRepository.findViewCountById(post.getId())).thenReturn(1);

        postViewService.increaseViewCount(SLUG);

        ArgumentCaptor<LocalDate> viewedOn = ArgumentCaptor.forClass(LocalDate.class);
        verify(postViewStatsRepository).increaseViewCount(any(), any(), viewedOn.capture());
        assertThat(viewedOn.getValue()).isEqualTo(LocalDate.now(KST));
    }

    @Test
    @DisplayName("집계 행의 id는 게시물 id와 다른 값")
    void increaseViewCount_shouldPassOwnIdForStatsRow() {
        Post post = post(0);
        when(postRepository.findBySlugAndIsHiddenFalse(SLUG)).thenReturn(Optional.of(post));
        when(postRepository.findViewCountById(post.getId())).thenReturn(1);

        postViewService.increaseViewCount(SLUG);

        ArgumentCaptor<UUID> statsId = ArgumentCaptor.forClass(UUID.class);
        verify(postViewStatsRepository).increaseViewCount(statsId.capture(), eq(post.getId()), any());
        assertThat(statsId.getValue()).isNotNull().isNotEqualTo(post.getId());
    }

    @Test
    @DisplayName("숨김이거나 없는 글이면 404, 아무것도 안 건드림")
    void increaseViewCount_whenNotFound_shouldThrow() {
        when(postRepository.findBySlugAndIsHiddenFalse(SLUG)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postViewService.increaseViewCount(SLUG)).isInstanceOf(PostNotFoundException.class);

        verify(postRepository, never()).increaseViewCount(any());
        verify(postViewStatsRepository, never()).increaseViewCount(any(), any(), any());
    }
}
