CREATE TABLE category (
    id         uuid         NOT NULL,
    title      varchar(255) NOT NULL,
    slug       varchar(255) NOT NULL,
    parent_id  uuid,
    created_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone,
    CONSTRAINT pk_category PRIMARY KEY (id),
    CONSTRAINT uk_category_slug UNIQUE (slug),
    CONSTRAINT fk_category_parent FOREIGN KEY (parent_id) REFERENCES category (id)
);

CREATE INDEX idx_category_parent_id ON category (parent_id);
