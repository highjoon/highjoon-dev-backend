package com.highjoondev.api.tag.repository;

import com.highjoondev.api.tag.entity.Tag;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TagRepository extends JpaRepository<Tag, UUID> {
    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, UUID id);

    Optional<Tag> findByName(String name);
}
