CREATE TABLE tag (
    id         uuid         NOT NULL,
    name       varchar(255) NOT NULL,
    created_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone,
    CONSTRAINT pk_tag PRIMARY KEY (id),
    CONSTRAINT uk_tag_name UNIQUE (name)
);
