package com.highjoondev.api.category.entity;

import com.highjoondev.api.category.exception.CategoryInvalidParentException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(indexes = @Index(name = "idx_category_parent_id", columnList = "parent_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, unique = true)
    private String slug;

    @ManyToOne(fetch = FetchType.LAZY)
    private Category parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.REMOVE)
    private List<Category> children = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    public static Category create(String title, String slug, Category parentCategory) {
        Category newCategory = new Category();
        newCategory.title = title;
        newCategory.slug = slug;
        newCategory.parent = parentCategory;
        if (parentCategory != null) {
            parentCategory.children.add(newCategory);
        }
        return newCategory;
    }

    public void update(String title, String slug, Category newParent) {
        if (newParent != null && wouldCycleWith(newParent)) {
            throw new CategoryInvalidParentException(this.id);
        }

        this.title = title;
        this.slug = slug;
        if (this.parent != null) {
            this.parent.children.remove(this);
        }
        this.parent = newParent;
        if (newParent != null) {
            newParent.children.add(this);
        }
    }

    private boolean wouldCycleWith(Category newParent) {
        for (Category cursor = newParent; cursor != null; cursor = cursor.getParent()) {
            if (this.id.equals(cursor.getId())) {
                return true;
            }
        }
        return false;
    }
}
