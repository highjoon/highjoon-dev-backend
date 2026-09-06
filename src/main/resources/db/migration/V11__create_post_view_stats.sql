CREATE TABLE post_view_stats
(
    id         uuid    NOT NULL,
    post_id    uuid    NOT NULL,
    viewed_on  date    NOT NULL,
    view_count integer NOT NULL DEFAULT 0,
    CONSTRAINT pk_post_view_stats PRIMARY KEY (id),
    CONSTRAINT uk_post_view_stats UNIQUE (post_id, viewed_on),
    CONSTRAINT fk_post_view_stats_post FOREIGN KEY (post_id) REFERENCES post (id) ON DELETE CASCADE
);