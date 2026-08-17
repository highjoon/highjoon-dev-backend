package com.highjoondev.api.global.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.highjoondev.api.tag.controller.TagController;
import com.highjoondev.api.tag.dto.TagCreateRequest;
import com.highjoondev.api.tag.dto.TagResponse;
import com.highjoondev.api.tag.dto.TagUpdateRequest;
import com.highjoondev.api.tag.service.TagService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TagController.class)
public class WriteApiBlockFilterTest {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    TagService tagService;

    @Test
    @DisplayName("POST 요청 시 403과 WRITE_NOT_ALLOWED")
    void post_shouldReturn403AndNotReachService() throws Exception {
        var request = new TagCreateRequest("react");

        mockMvc.perform(post("/api/v1/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("WRITE_NOT_ALLOWED"));

        verify(tagService, never()).create(any());
    }

    @Test
    @DisplayName("PUT 요청 시 403과 WRITE_NOT_ALLOWED")
    void put_shouldReturn403AndNotReachService() throws Exception {
        UUID id = UUID.randomUUID();
        var request = new TagUpdateRequest("react");

        mockMvc.perform(put("/api/v1/tags/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("WRITE_NOT_ALLOWED"));

        verify(tagService, never()).updateById(any(), any());
    }

    @Test
    @DisplayName("DELETE 요청 시 403과 WRITE_NOT_ALLOWED")
    void delete_shouldReturn403AndNotReachService() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/tags/{id}", id))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("WRITE_NOT_ALLOWED"));

        verify(tagService, never()).deleteById(any());
    }

    @Test
    @DisplayName("GET 요청은 차단 안 함")
    void get_shouldReachService() throws Exception {
        when(tagService.findAll()).thenReturn(List.of(new TagResponse(UUID.randomUUID(), "react", 0, Instant.now())));

        mockMvc.perform(get("/api/v1/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("react"));

        verify(tagService).findAll();
    }
}
