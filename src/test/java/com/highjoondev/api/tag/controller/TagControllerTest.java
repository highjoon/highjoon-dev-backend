package com.highjoondev.api.tag.controller;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.highjoondev.api.tag.dto.TagCreateRequest;
import com.highjoondev.api.tag.dto.TagResponse;
import com.highjoondev.api.tag.dto.TagUpdateRequest;
import com.highjoondev.api.tag.exception.DuplicatedTagNameException;
import com.highjoondev.api.tag.exception.TagNotFoundException;
import com.highjoondev.api.tag.service.TagService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TagController.class)
public class TagControllerTest {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    TagService tagService;

    @Test
    void create_withValidRequest_shouldReturn201() throws Exception {
        // Given
        var request = new TagCreateRequest("react");
        var response = new TagResponse(UUID.randomUUID(), "react", Instant.now());
        when(tagService.create(request)).thenReturn(response);

        // When / Then
        mockMvc.perform(post("/api/v1/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/v1/tags/" + response.id())))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("react"));
    }

    @Test
    void create_withBlankName_shouldReturn400() throws Exception {
        var request = new TagCreateRequest("");

        mockMvc.perform(post("/api/v1/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    void create_withDuplicateName_shouldReturn409() throws Exception {
        var request = new TagCreateRequest("react");
        when(tagService.create(request)).thenThrow(new DuplicatedTagNameException("react"));

        mockMvc.perform(post("/api/v1/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("TAG_DUPLICATED_NAME"));
    }

    @Test
    void findById_whenFound_shouldReturn200() throws Exception {
        UUID id = UUID.randomUUID();
        when(tagService.findById(id)).thenReturn(new TagResponse(id, "react", Instant.now()));

        mockMvc.perform(get("/api/v1/tags/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(id.toString()))
                .andExpect(jsonPath("$.data.name").value("react"));
    }

    @Test
    void findById_whenNotFound_shouldReturn404() throws Exception {
        UUID id = UUID.randomUUID();
        when(tagService.findById(id)).thenThrow(new TagNotFoundException(id));

        mockMvc.perform(get("/api/v1/tags/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("TAG_NOT_FOUND"));
    }

    @Test
    void findById_withInvalidUuid_shouldReturn400() throws Exception {
        mockMvc.perform(get("/api/v1/tags/{id}", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_PARAMETER"));
    }

    @Test
    void findAll_shouldReturn200WithTags() throws Exception {
        when(tagService.findAll()).thenReturn(List.of(new TagResponse(UUID.randomUUID(), "react", Instant.now())));

        mockMvc.perform(get("/api/v1/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].name").value("react"));
    }

    @Test
    void findAll_whenEmpty_shouldReturn200WithEmptyList() throws Exception {
        when(tagService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void update_shouldReturn200() throws Exception {
        UUID id = UUID.randomUUID();
        var request = new TagUpdateRequest("react");
        when(tagService.updateById(id, request)).thenReturn(new TagResponse(id, "react", Instant.now()));

        mockMvc.perform(put("/api/v1/tags/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(id.toString()));
    }

    @Test
    void update_whenNotFound_shouldReturn404() throws Exception {
        UUID id = UUID.randomUUID();
        var request = new TagUpdateRequest("react");
        when(tagService.updateById(id, request)).thenThrow(new TagNotFoundException(id));

        mockMvc.perform(put("/api/v1/tags/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("TAG_NOT_FOUND"));
    }

    @Test
    void update_withBlankName_shouldReturn400() throws Exception {
        UUID id = UUID.randomUUID();
        var request = new TagUpdateRequest("");

        mockMvc.perform(put("/api/v1/tags/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    void update_withDuplicateName_shouldReturn409() throws Exception {
        UUID id = UUID.randomUUID();
        var request = new TagUpdateRequest("react");
        when(tagService.updateById(id, request)).thenThrow(new DuplicatedTagNameException("react"));

        mockMvc.perform(put("/api/v1/tags/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("TAG_DUPLICATED_NAME"));
    }

    @Test
    void delete_shouldReturn200() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/tags/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void delete_whenNotFound_shouldReturn404() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new TagNotFoundException(id)).when(tagService).deleteById(id);

        mockMvc.perform(delete("/api/v1/tags/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("TAG_NOT_FOUND"));
    }
}
