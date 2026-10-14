package com.highjoondev.api.post.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.highjoondev.api.TestcontainersConfig;
import com.highjoondev.api.post.entity.Post;
import com.highjoondev.api.post.entity.PostViewStats;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfig.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class PostViewStatsConcurrencyTest {

    private static final LocalDate DAY = LocalDate.of(2026, 5, 11);
    private static final int THREADS = 16;
    private static final int PER_THREAD = 25;

    @Autowired
    PostViewStatsRepository postViewStatsRepository;

    @Autowired
    PostRepository postRepository;

    @Autowired
    PlatformTransactionManager transactionManager;

    @AfterEach
    void cleanUp() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> postRepository.deleteAllInBatch());
    }

    @Test
    @DisplayName("Row가 없는 상태에서 여러 스레드가 동시에 조회수를 올려도 유실 없이 한 Row에 누적")
    void increaseViewCount_concurrentNoLostUpdate() throws Exception {
        Post post = postRepository.saveAndFlush(Post.builder()
                .slug("concurrent")
                .title("제목")
                .description("설명")
                .contentUrl("https://example.com/concurrent")
                .bannerImageUrl("https://example.com/concurrent.png")
                .publishedAt(Instant.parse("2026-05-11T00:00:00Z"))
                .build());
        UUID postId = post.getId();

        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        CountDownLatch ready = new CountDownLatch(THREADS);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Void>> futures = new ArrayList<>();

        for (int t = 0; t < THREADS; t++) {
            Callable<Void> task = () -> {
                ready.countDown();
                start.await();
                for (int i = 0; i < PER_THREAD; i++) {
                    tx.executeWithoutResult(
                            status -> postViewStatsRepository.increaseViewCount(UUID.randomUUID(), postId, DAY));
                }
                return null;
            };
            futures.add(pool.submit(task));
        }

        ready.await();
        start.countDown();
        for (Future<Void> future : futures) {
            future.get(60, TimeUnit.SECONDS);
        }
        pool.shutdown();

        List<PostViewStats> stats = postViewStatsRepository.findAll();
        assertThat(stats).hasSize(1);
        assertThat(stats.getFirst().getViewCount()).isEqualTo(THREADS * PER_THREAD);
    }
}
