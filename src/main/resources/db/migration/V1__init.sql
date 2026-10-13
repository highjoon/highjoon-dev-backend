CREATE TABLE category
(
    id         BINARY(16)                   NOT NULL,
    title      VARCHAR(255)                 NOT NULL,
    slug       VARCHAR(255) COLLATE utf8mb4_bin NOT NULL,
    parent_id  BINARY(16),
    created_at DATETIME(6)                  NOT NULL,
    updated_at DATETIME(6)                  NOT NULL,
    CONSTRAINT pk_category PRIMARY KEY (id),
    CONSTRAINT uk_category_slug UNIQUE (slug),
    INDEX idx_category_parent_id (parent_id),
    CONSTRAINT fk_category_parent FOREIGN KEY (parent_id) REFERENCES category (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE tag
(
    id         BINARY(16)                   NOT NULL,
    name       VARCHAR(255) COLLATE utf8mb4_bin NOT NULL,
    created_at DATETIME(6)                  NOT NULL,
    updated_at DATETIME(6)                  NOT NULL,
    CONSTRAINT pk_tag PRIMARY KEY (id),
    CONSTRAINT uk_tag_name UNIQUE (name)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE post
(
    id               BINARY(16)                   NOT NULL,
    slug             VARCHAR(255) COLLATE utf8mb4_bin NOT NULL,
    title            VARCHAR(255)                 NOT NULL,
    description      TEXT                         NOT NULL,
    content_url      TEXT                         NOT NULL,
    banner_image_url TEXT                         NOT NULL,
    published_at     DATETIME(6)                  NOT NULL,
    view_count       INT                          NOT NULL DEFAULT 0,
    is_featured      TINYINT(1)                   NOT NULL DEFAULT 0,
    is_hidden        TINYINT(1)                   NOT NULL DEFAULT 0,
    category_id      BINARY(16),
    created_at       DATETIME(6)                  NOT NULL,
    updated_at       DATETIME(6)                  NOT NULL,
    CONSTRAINT pk_post PRIMARY KEY (id),
    CONSTRAINT uk_post_slug UNIQUE (slug),
    INDEX idx_post_category_id (category_id),
    INDEX idx_post_published_at (published_at DESC),
    CONSTRAINT fk_post_category FOREIGN KEY (category_id) REFERENCES category (id) ON DELETE SET NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE post_tag
(
    id         BINARY(16)  NOT NULL,
    post_id    BINARY(16)  NOT NULL,
    tag_id     BINARY(16)  NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_post_tag PRIMARY KEY (id),
    CONSTRAINT uk_post_tag UNIQUE (post_id, tag_id),
    INDEX idx_post_tag_tag_id (tag_id),
    CONSTRAINT fk_post_tag_post FOREIGN KEY (post_id) REFERENCES post (id) ON DELETE CASCADE,
    CONSTRAINT fk_post_tag_tag FOREIGN KEY (tag_id) REFERENCES tag (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE post_view_stats
(
    id         BINARY(16) NOT NULL,
    post_id    BINARY(16) NOT NULL,
    viewed_on  DATE       NOT NULL,
    view_count INT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_post_view_stats PRIMARY KEY (id),
    CONSTRAINT uk_post_view_stats UNIQUE (post_id, viewed_on),
    CONSTRAINT fk_post_view_stats_post FOREIGN KEY (post_id) REFERENCES post (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
