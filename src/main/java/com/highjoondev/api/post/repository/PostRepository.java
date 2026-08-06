package com.highjoondev.api.post.repository;

import com.highjoondev.api.post.entity.Post;
import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, UUID> {
    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, UUID id);

    /** 수정, 삭제도 쓰는 조회라 숨김 여부로 거르지 않음 */
    @EntityGraph(attributePaths = {"category", "category.parent"})
    Optional<Post> findBySlug(String slug);

    /** 추천 자리가 찼는지 검사용. 숨긴 글도 포함해서 조회 */
    Optional<Post> findFirstByIsFeaturedTrue();

    /**
     * 추천 글 1건
     * - 만약 추천 글이 2건 이상이면, 가장 최근에 발행된 글 반환
     */
    @EntityGraph(attributePaths = {"category", "category.parent"})
    Optional<Post> findFirstByIsFeaturedTrueAndIsHiddenFalseOrderByPublishedAtDescIdDesc();

    /**
     * 목록
     * - 발행일, id 순으로 정렬
     */
    @EntityGraph(attributePaths = {"category", "category.parent"})
    Page<Post> findByIsHiddenFalseOrderByPublishedAtDescIdDesc(Pageable pageable);

    /**
     * 이전 글
     * - 현재 글보다 먼저 발행된 것 중에서 가장 최근 글
     */
    @Query("""
        SELECT p FROM Post p
        WHERE p.isHidden = false
          AND (p.publishedAt < :publishedAt
               OR (p.publishedAt = :publishedAt AND p.id < :id))
        ORDER BY p.publishedAt DESC, p.id DESC
        LIMIT 1
        """)
    Optional<Post> findPreviousPost(@Param("publishedAt") Instant publishedAt, @Param("id") UUID id);

    /**
     * 다음 글
     * - 현재 글보다 나중에 발행된 것 중에서 가장 빠른 글
     */
    @Query("""
        SELECT p FROM Post p
        WHERE p.isHidden = false
          AND (p.publishedAt > :publishedAt
               OR (p.publishedAt = :publishedAt AND p.id > :id))
        ORDER BY p.publishedAt ASC, p.id ASC
        LIMIT 1
        """)
    Optional<Post> findNextPost(@Param("publishedAt") Instant publishedAt, @Param("id") UUID id);

    /**
     * 카테고리 별 목록
     * @param categoryIds 카테고리 ID 목록
     * @param pageable 페이지네이션
     */
    @EntityGraph(attributePaths = {"category", "category.parent"})
    Page<Post> findByIsHiddenFalseAndCategoryIdInOrderByPublishedAtDescIdDesc(
            Collection<UUID> categoryIds, Pageable pageable);
}
