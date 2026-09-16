package com.highjoondev.api.post.dto;

import java.util.UUID;

/** 사이드바 메뉴용 게시물 한 행. 쿼리 별칭과 게터 이름이 대응 */
public interface PostMenuRow {
    UUID getCategoryId();

    String getTitle();

    String getSlug();
}
