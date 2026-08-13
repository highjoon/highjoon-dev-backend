package com.highjoondev.api.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.highjoondev.api.global.exception.CommonErrorCode;
import com.highjoondev.api.global.response.ApiResult;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 인증 기능 구현 전까지 쓰기 요청을 막는 임시 필터
 * - app.write-api.enabled=true를 주면 빈이 안 만들어져서 통과
 * - 키가 없으면 막는 쪽이 기본 값
 */
@Slf4j
@Component
@Order(2) // 차단된 요청도 로그에 남도록 RequestLoggingFilter 다음에 실행
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.write-api.enabled", havingValue = "false", matchIfMissing = true)
public class WriteApiBlockFilter extends OncePerRequestFilter {
    private static final Set<String> BLOCKED_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");
    private static final String API_PREFIX = "/api/";

    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        boolean isApiRequest = request.getRequestURI().startsWith(request.getContextPath() + API_PREFIX);
        boolean isWriteMethod = BLOCKED_METHODS.contains(request.getMethod());

        return !isApiRequest || !isWriteMethod;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        log.warn("쓰기 요청 차단: {} {}", request.getMethod(), request.getRequestURI());

        response.setStatus(CommonErrorCode.WRITE_NOT_ALLOWED.status().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(
                response.getWriter(),
                ApiResult.error(CommonErrorCode.WRITE_NOT_ALLOWED.code(), CommonErrorCode.WRITE_NOT_ALLOWED.message()));
    }
}
