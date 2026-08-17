package com.highjoondev.api.tag.repository;

import com.highjoondev.api.tag.dto.TagResponse;
import com.highjoondev.api.tag.entity.Tag;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TagRepository extends JpaRepository<Tag, UUID> {
    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, UUID id);

    Optional<Tag> findByName(String name);

    /** 태그 목록. 글이 없는 태그도 0으로 반환 */
    @Query("""
        SELECT new com.highjoondev.api.tag.dto.TagResponse(t.id, t.name, COUNT(pt.id), t.createdAt)
        FROM Tag t
        LEFT JOIN PostTag pt ON pt.tag = t AND pt.post.isHidden = false
        GROUP BY t.id, t.name, t.createdAt
        ORDER BY t.name
        """)
    List<TagResponse> findAllWithPostCount();

    /** 태그 하나의 글 수. 숨긴 글 제외 */
    @Query("SELECT COUNT(pt.id) FROM PostTag pt WHERE pt.tag.id = :tagId AND pt.post.isHidden = false")
    long countPostsByTagId(@Param("tagId") UUID tagId);
}
