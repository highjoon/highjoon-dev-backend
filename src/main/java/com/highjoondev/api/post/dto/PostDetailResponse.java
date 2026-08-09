package com.highjoondev.api.post.dto;

public record PostDetailResponse(PostResponse post, PostSummary previous, PostSummary next) {}
