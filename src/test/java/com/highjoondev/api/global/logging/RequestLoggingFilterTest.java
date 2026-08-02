package com.highjoondev.api.global.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.highjoondev.api.category.controller.CategoryController;
import com.highjoondev.api.category.service.CategoryService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CategoryController.class)
public class RequestLoggingFilterTest {
    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    CategoryService categoryService;

    @Test
    void response_shouldContainTraceIdHeader() throws Exception {
        when(categoryService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(header().string(RequestLoggingFilter.TRACE_ID_HEADER, matchesPattern("[0-9a-f]{8}")));
    }

    @Test
    void actuatorRequest_shouldNotContainTraceIdHeader() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(header().doesNotExist(RequestLoggingFilter.TRACE_ID_HEADER));
    }

    @Test
    void corsResponse_shouldExposeTraceIdHeader() throws Exception {
        when(categoryService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/categories").header("Origin", "http://localhost:3000"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                                "Access-Control-Expose-Headers", containsString(RequestLoggingFilter.TRACE_ID_HEADER)));
    }

    @Test
    void maskIp_shouldRemoveLastOctet() {
        assertThat(RequestLoggingFilter.maskIp("192.168.1.123")).isEqualTo("192.168.1.0");
    }

    @Test
    void maskIp_whenNull_shouldReturnPlaceholder() {
        assertThat(RequestLoggingFilter.maskIp(null)).isEqualTo("-");
    }

    @Test
    void truncate_whenWithinLimit_shouldKeepAsIs() {
        String userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)";

        assertThat(RequestLoggingFilter.truncate(userAgent)).isEqualTo(userAgent);
    }

    @Test
    void truncate_whenLongerThanLimit_shouldCutAndMark() {
        String userAgent = "a".repeat(300);

        assertThat(RequestLoggingFilter.truncate(userAgent)).hasSize(259).endsWith("...");
    }
}
