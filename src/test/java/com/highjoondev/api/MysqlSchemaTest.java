package com.highjoondev.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.highjoondev.api.category.entity.Category;
import com.highjoondev.api.category.repository.CategoryRepository;
import com.highjoondev.api.post.entity.Post;
import com.highjoondev.api.post.repository.PostRepository;
import com.highjoondev.api.tag.entity.Tag;
import com.highjoondev.api.tag.repository.TagRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfig.class)
class MysqlSchemaTest {

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    TagRepository tagRepository;

    @Autowired
    PostRepository postRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    private Post post(String slug, Instant publishedAt, boolean isFeatured, boolean isHidden) {
        return Post.builder()
                .slug(slug)
                .title("제목")
                .description("설명")
                .contentUrl("https://example.com/" + slug)
                .bannerImageUrl("https://example.com/" + slug + ".png")
                .publishedAt(publishedAt)
                .isFeatured(isFeatured)
                .isHidden(isHidden)
                .build();
    }

    @Test
    @DisplayName("대소문자만 다른 태그 이름, 카테고리 slug, 글 slug를 따로 저장")
    void uniqueColumns_areCaseSensitive() {
        tagRepository.saveAndFlush(Tag.builder().name("Java").build());
        tagRepository.saveAndFlush(Tag.builder().name("java").build());
        categoryRepository.saveAndFlush(
                Category.builder().title("대문자").slug("Spring").build());
        categoryRepository.saveAndFlush(
                Category.builder().title("소문자").slug("spring").build());
        postRepository.saveAndFlush(post("Slug", Instant.parse("2026-05-11T00:00:00Z"), false, false));
        postRepository.saveAndFlush(post("slug", Instant.parse("2026-05-11T00:00:00Z"), false, false));

        assertThat(tagRepository.count()).isEqualTo(2);
        assertThat(categoryRepository.count()).isEqualTo(2);
        assertThat(postRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("스왑 없는 UUID_TO_BIN 으로 적재한 행을 JPA 가 같은 UUID 로 읽음")
    void uuidToBin_matchesHibernateByteOrder() {
        UUID id = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        jdbcTemplate.update(
                "INSERT INTO tag (id, name, created_at, updated_at) VALUES (UUID_TO_BIN(?), 'bin', NOW(6), NOW(6))",
                id.toString());

        assertThat(tagRepository.findById(id)).isPresent();
    }
}
