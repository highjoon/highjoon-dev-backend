package com.highjoondev.api.post.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 어드민의 조회수 추이 화면이 읽을 기록 */
@Entity
@Table(
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_post_view_stats",
                        columnNames = {"post_id", "viewed_on"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostViewStats {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private Post post;

    @Column(nullable = false)
    private LocalDate viewedOn;

    @Column(nullable = false)
    private int viewCount;

    @Builder
    private PostViewStats(Post post, LocalDate viewedOn, int viewCount) {
        this.post = post;
        this.viewedOn = viewedOn;
        this.viewCount = viewCount;
    }
}
