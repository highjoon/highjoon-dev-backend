package com.highjoondev.api.category.repository;

import com.highjoondev.api.category.dto.CategoryMenuRow;
import com.highjoondev.api.category.entity.Category;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, UUID id);

    Optional<Category> findBySlug(String slug);

    /** 사이드바 메뉴용 카테고리 행 목록 */
    @Query("SELECT c.id AS id, c.title AS title, c.slug AS slug, c.parent.id AS parentId FROM Category c")
    List<CategoryMenuRow> findMenuRows();
}
