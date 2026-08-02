package com.highjoondev.api.global.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {
    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String TRACE_ID = "traceId";
    private static final String CLIENT_IP = "clientIp";
    private static final String USER_AGENT = "userAgent";
    private static final int USER_AGENT_MAX_LENGTH = 256;
    private static final String EMPTY_VALUE = "-";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/actuator");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String traceId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put(TRACE_ID, traceId);
        // forward-headers-strategy: native 라서 톰캣이 X-Forwarded-For를 처리한 실제 IP가 들어온다
        MDC.put(CLIENT_IP, maskIp(request.getRemoteAddr()));
        MDC.put(USER_AGENT, truncate(request.getHeader("User-Agent")));
        response.setHeader(TRACE_ID_HEADER, traceId);

        long startedAt = System.nanoTime();
        try {
            chain.doFilter(request, response);
        } finally {
            long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000;
            log.info("{} {} {} {}ms", request.getMethod(), pathWithQuery(request), response.getStatus(), elapsedMs);
            MDC.clear();
        }
    }

    // 마지막 자리를 지운다. 같은 대역에서 계속 두드리는 건 보이고 개인은 못 짚는다
    static String maskIp(String ip) {
        if (ip == null) {
            return EMPTY_VALUE;
        }

        int lastDot = ip.lastIndexOf('.');
        return lastDot < 0 ? ip : ip.substring(0, lastDot) + ".0";
    }

    // UA는 클라이언트가 정하는 값이라 제한이 없으면 긴 줄이 수집 단계에서 통째로 버려진다
    static String truncate(String userAgent) {
        if (userAgent == null) {
            return EMPTY_VALUE;
        }

        return userAgent.length() <= USER_AGENT_MAX_LENGTH
                ? userAgent
                : userAgent.substring(0, USER_AGENT_MAX_LENGTH) + "...";
    }

    private String pathWithQuery(HttpServletRequest request) {
        String query = request.getQueryString();
        return query == null ? request.getRequestURI() : request.getRequestURI() + "?" + query;
    }
}
