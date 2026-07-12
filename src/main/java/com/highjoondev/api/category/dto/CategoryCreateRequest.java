package com.highjoondev.api.category.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record CategoryCreateRequest(@NotBlank String title, UUID parentId) {}
