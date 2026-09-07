package com.highjoondev.api.post.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.highjoondev.api.TestcontainersConfig;
import com.highjoondev.api.post.entity.Post;
import com.highjoondev.api.post.entity.PostViewStats;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfig.class)
public class PostViewStatsRepositoryTest {

    private static final LocalDate DAY_1 = LocalDate.of(2026, 5, 11);
    private static final LocalDate DAY_2 = LocalDate.of(2026, 5, 12);

    @Autowired
    PostViewStatsRepository postViewStatsRepository;

    @Autowired
    PostRepository postRepository;

    @Autowired
    EntityManager entityManager;

    private Post savedPost;

    // V7이 넣은 이관 글이 섞이지 않도록 비움. 테스트 종료 시 롤백
    @BeforeEach
    void setUp() {
        postRepository.deleteAllInBatch();
        savedPost = postRepository.saveAndFlush(Post.builder()
                .slug("view-stats")
                .title("제목")
                .description("설명")
                .contentUrl("https://example.com/view-stats")
                .bannerImageUrl("https://example.com/view-stats.png")
                .publishedAt(Instant.parse("2026-05-11T00:00:00Z"))
                .build());
    }

    private void increase(LocalDate viewedOn) {
        postViewStatsRepository.increaseViewCount(UUID.randomUUID(), savedPost.getId(), viewedOn);
    }

    // 네이티브 쿼리 결과는 영속성 컨텍스트가 모르므로 비우고 다시 읽음
    private List<PostViewStats> readAll() {
        entityManager.clear();
        return postViewStatsRepository.findAll();
    }

    @Test
    @DisplayName("그 날 첫 조회면 1로 시작하는 행 생성")
    void increaseViewCount_firstOfDay() {
        increase(DAY_1);

        List<PostViewStats> stats = readAll();

        assertThat(stats).hasSize(1);
        assertThat(stats.getFirst().getViewCount()).isEqualTo(1);
        assertThat(stats.getFirst().getViewedOn()).isEqualTo(DAY_1);
        assertThat(stats.getFirst().getPost().getId()).isEqualTo(savedPost.getId());
    }

    @Test
    @DisplayName("같은 날 다시 부르면 행 하나에 누적")
    void increaseViewCount_sameDayAccumulates() {
        increase(DAY_1);
        increase(DAY_1);
        increase(DAY_1);

        List<PostViewStats> stats = readAll();

        assertThat(stats).hasSize(1);
        assertThat(stats.getFirst().getViewCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("날짜가 다르면 행이 따로 생김")
    void increaseViewCount_otherDayCreatesRow() {
        increase(DAY_1);
        increase(DAY_1);
        increase(DAY_2);

        List<PostViewStats> stats = readAll();

        assertThat(stats).hasSize(2);
        assertThat(stats)
                .extracting(PostViewStats::getViewedOn, PostViewStats::getViewCount)
                .containsExactlyInAnyOrder(tuple(DAY_1, 2), tuple(DAY_2, 1));
    }

    @Test
    @DisplayName("글을 지우면 집계도 함께 삭제")
    void deletePost_shouldCascadeStats() {
        increase(DAY_1);
        entityManager.clear();

        postRepository.deleteAllInBatch();

        assertThat(readAll()).isEmpty();
    }
}
