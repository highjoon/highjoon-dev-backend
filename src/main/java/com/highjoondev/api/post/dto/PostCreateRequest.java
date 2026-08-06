package com.highjoondev.api.post.dto;

import com.highjoondev.api.category.entity.Category;
import com.highjoondev.api.post.entity.Post;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public record PostCreateRequest(
        @Schema(description = "제목", example = "Spring Data JPA 파헤치기")
        @NotBlank(message = "제목은 필수입니다") @Size(max = 255, message = "제목은 255자를 넘을 수 없습니다") String title,

        @Schema(description = "Slug", example = "spring-data-jpa")
        @NotBlank(message = "slug은 필수입니다") @Size(max = 255, message = "slug은 255자를 넘을 수 없습니다") String slug,

        @Schema(description = "요약") @NotBlank(message = "요약은 필수입니다") String description,

        @Schema(description = "본문 파일 URL") @NotBlank(message = "본문 URL은 필수입니다") String contentUrl,

        @Schema(description = "배너 이미지 URL") @NotBlank(message = "배너 이미지 URL은 필수입니다") String bannerImageUrl,

        @Schema(description = "발행일") @NotNull(message = "발행일은 필수입니다") Instant publishedAt,

        @Schema(description = "카테고리 ID (없으면 미분류)") UUID categoryId,

        @Schema(description = "추천 글 여부", defaultValue = "false")
        boolean isFeatured,

        @Schema(description = "숨김 여부", defaultValue = "false")
        boolean isHidden) {
    public Post toEntity(Category category) {
        return Post.builder()
                .title(title)
                .slug(slug)
                .description(description)
                .contentUrl(contentUrl)
                .bannerImageUrl(bannerImageUrl)
                .publishedAt(publishedAt)
                .category(category)
                .isFeatured(isFeatured)
                .isHidden(isHidden)
                .build();
    }
}
