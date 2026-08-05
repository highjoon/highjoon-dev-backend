package com.highjoondev.api.post.entity;

import com.highjoondev.api.category.entity.Category;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Getter
@Table(
        indexes = {
            @Index(name = "idx_post_category_id", columnList = "category_id"),
            @Index(name = "idx_post_published_at", columnList = "published_at DESC"),
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @Column(nullable = false, columnDefinition = "text")
    private String contentUrl;

    @Column(nullable = false, columnDefinition = "text")
    private String bannerImageUrl;

    @Column(nullable = false)
    private Instant publishedAt;

    @Column(nullable = false)
    private int viewCount;

    @Column(nullable = false)
    private boolean isFeatured;

    @Column(nullable = false)
    private boolean isHidden;

    @ManyToOne(fetch = FetchType.LAZY)
    private Category category;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    @Builder
    private Post(
            String slug,
            String title,
            String description,
            String contentUrl,
            String bannerImageUrl,
            Instant publishedAt,
            boolean isFeatured,
            boolean isHidden,
            Category category) {
        this.slug = slug;
        this.title = title;
        this.description = description;
        this.contentUrl = contentUrl;
        this.bannerImageUrl = bannerImageUrl;
        this.publishedAt = publishedAt;
        this.isFeatured = isFeatured;
        this.isHidden = isHidden;
        this.category = category;
    }

    public void update(
            String slug,
            String title,
            String description,
            String contentUrl,
            String bannerImageUrl,
            Instant publishedAt,
            Category category) {
        this.slug = slug;
        this.title = title;
        this.description = description;
        this.contentUrl = contentUrl;
        this.bannerImageUrl = bannerImageUrl;
        this.publishedAt = publishedAt;
        this.category = category;
    }

    public void updateIsFeatured(boolean featured) {
        this.isFeatured = featured;
    }

    public void updateIsHidden(boolean hidden) {
        this.isHidden = hidden;
    }
}
