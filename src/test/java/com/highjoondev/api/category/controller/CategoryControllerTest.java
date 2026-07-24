package com.highjoondev.api.category.controller;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.highjoondev.api.category.dto.CategoryCreateRequest;
import com.highjoondev.api.category.dto.CategoryResponse;
import com.highjoondev.api.category.dto.CategoryUpdateRequest;
import com.highjoondev.api.category.exception.CategoryInvalidParentException;
import com.highjoondev.api.category.exception.CategoryNotFoundException;
import com.highjoondev.api.category.exception.CategoryParentNotFoundException;
import com.highjoondev.api.category.exception.DuplicatedCategorySlugException;
import com.highjoondev.api.category.service.CategoryService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CategoryController.class)
public class CategoryControllerTest {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    CategoryService categoryService;

    @Test
    void create_withValidRequest_shouldReturn201() throws Exception {
        // Given
        var request = new CategoryCreateRequest("title", "slug", null);
        var response = new CategoryResponse(UUID.randomUUID(), "title", "slug", null, Instant.now());
        when(categoryService.create(request)).thenReturn(response);

        // When / Then
        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("title"));
    }

    @Test
    void create_withBlankTitle_shouldReturn400() throws Exception {
        var request = new CategoryCreateRequest("", "slug", null);

        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    void create_withNonExistentParentId_shouldReturn400() throws Exception {
        UUID parentId = UUID.randomUUID();
        var request = new CategoryCreateRequest("title", "slug", parentId);
        when(categoryService.create(request)).thenThrow(new CategoryParentNotFoundException(parentId));

        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("CATEGORY_PARENT_NOT_FOUND"));
    }

    @Test
    void create_withDuplicateSlug_shouldReturn409() throws Exception {
        var request = new CategoryCreateRequest("title", "slug", null);
        when(categoryService.create(request)).thenThrow(new DuplicatedCategorySlugException("slug"));

        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("DUPLICATED_SLUG"));
    }

    @Test
    void findById_whenNotFound_shouldReturn404() throws Exception {
        UUID id = UUID.randomUUID();
        when(categoryService.findById(id)).thenThrow(new CategoryNotFoundException(id));

        mockMvc.perform(get("/api/v1/categories/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("CATEGORY_NOT_FOUND"));
    }

    @Test
    void findById_whenFound_shouldReturn200() throws Exception {
        UUID id = UUID.randomUUID();
        var response = new CategoryResponse(id, "title", "slug", null, Instant.now());
        when(categoryService.findById(id)).thenReturn(response);

        mockMvc.perform(get("/api/v1/categories/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(id.toString()))
                .andExpect(jsonPath("$.data.title").value("title"));
    }

    @Test
    void findById_withInvalidUuid_shouldReturn400() throws Exception {
        mockMvc.perform(get("/api/v1/categories/{id}", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_PARAMETER"));
    }

    @Test
    void findAll_shouldReturn200WithCategories() throws Exception {
        var response = new CategoryResponse(UUID.randomUUID(), "title", "slug", null, Instant.now());
        when(categoryService.findAll()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].title").value("title"));
    }

    @Test
    void findAll_whenEmpty_shouldReturn200WithEmptyList() throws Exception {
        when(categoryService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void update_shouldReturn200() throws Exception {
        UUID id = UUID.randomUUID();
        UUID parentId = UUID.randomUUID();
        var request = new CategoryUpdateRequest("title", "slug", parentId);
        when(categoryService.updateById(id, request))
                .thenReturn(new CategoryResponse(id, "title", "slug", parentId, Instant.now()));

        mockMvc.perform(put("/api/v1/categories/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(id.toString()));
    }

    @Test
    void update_whenInvalidId_shouldReturn404() throws Exception {
        UUID id = UUID.randomUUID();
        var request = new CategoryUpdateRequest("title", "slug", id);
        when(categoryService.updateById(id, request)).thenThrow(new CategoryNotFoundException(id));

        mockMvc.perform(put("/api/v1/categories/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("CATEGORY_NOT_FOUND"));
    }

    @Test
    void update_whenInvalidParentId_shouldReturn400() throws Exception {
        UUID id = UUID.randomUUID();
        UUID parentId = UUID.randomUUID();
        var request = new CategoryUpdateRequest("title", "slug", parentId);
        when(categoryService.updateById(id, request)).thenThrow(new CategoryParentNotFoundException(parentId));

        mockMvc.perform(put("/api/v1/categories/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("CATEGORY_PARENT_NOT_FOUND"));
    }

    @Test
    void update_whenSelfAsParentId_shouldReturn400() throws Exception {
        UUID id = UUID.randomUUID();
        var request = new CategoryUpdateRequest("title", "slug", id);
        when(categoryService.updateById(id, request)).thenThrow(new CategoryInvalidParentException(id));

        mockMvc.perform(put("/api/v1/categories/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("CATEGORY_INVALID_PARENT"));
    }

    @Test
    void update_whenInvalidTitle_shouldReturn400() throws Exception {
        UUID id = UUID.randomUUID();
        var request = new CategoryUpdateRequest("", "slug", null);

        mockMvc.perform(put("/api/v1/categories/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    void update_withDuplicateSlug_shouldReturn409() throws Exception {
        UUID id = UUID.randomUUID();
        var request = new CategoryUpdateRequest("title", "slug", null);
        when(categoryService.updateById(id, request)).thenThrow(new DuplicatedCategorySlugException("slug"));

        mockMvc.perform(put("/api/v1/categories/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("DUPLICATED_SLUG"));
    }

    @Test
    void delete_shouldReturn200() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/categories/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void delete_whenNotFound_shouldReturn404() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new CategoryNotFoundException(id)).when(categoryService).deleteById(id);

        mockMvc.perform(delete("/api/v1/categories/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("CATEGORY_NOT_FOUND"));
    }
}
