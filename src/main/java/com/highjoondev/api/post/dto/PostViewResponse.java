package com.highjoondev.api.post.dto;

public record PostViewResponse(int viewCount) {
    public static PostViewResponse from(int viewCount) {
        return new PostViewResponse(viewCount);
    }
}
