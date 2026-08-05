CREATE TABLE post
(
    id               uuid                        NOT NULL,
    slug             varchar(255)                NOT NULL,
    title            varchar(255)                NOT NULL,
    description      text                        NOT NULL,
    content_url      text                        NOT NULL,
    banner_image_url text                        NOT NULL,
    published_at     timestamp(6) with time zone NOT NULL,
    view_count       integer                     NOT NULL DEFAULT 0,
    is_featured      boolean                     NOT NULL DEFAULT false,
    is_hidden        boolean                     NOT NULL DEFAULT false,
    category_id      uuid,
    created_at       timestamp(6) with time zone NOT NULL,
    updated_at       timestamp(6) with time zone NOT NULL,
    CONSTRAINT pk_post PRIMARY KEY (id),
    CONSTRAINT uk_post_slug UNIQUE (slug),
    CONSTRAINT fk_post_category FOREIGN KEY (category_id) REFERENCES category (id) ON DELETE SET NULL
);

CREATE INDEX idx_post_category_id ON post (category_id);
CREATE INDEX idx_post_published_at ON post (published_at DESC);
