package com.highjoondev.api.post.repository;

import com.highjoondev.api.post.entity.PostViewStats;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostViewStatsRepository extends JpaRepository<PostViewStats, UUID> {

    /** 해당 날짜의 집계를 1 올림. 그 날 첫 조회면 1로 시작하는 Row 생성 */
    @Modifying
    @Query(nativeQuery = true, value = """
        INSERT INTO post_view_stats (id, post_id, viewed_on, view_count)
        VALUES (:id, :postId, :viewedOn, 1)
        ON CONFLICT (post_id, viewed_on)
        DO UPDATE SET view_count = post_view_stats.view_count + 1
        """)
    void increaseViewCount(@Param("id") UUID id, @Param("postId") UUID postId, @Param("viewedOn") LocalDate viewedOn);
}
