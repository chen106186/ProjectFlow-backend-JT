package com.jitong.projectflow.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jitong.projectflow.common.api.ApiResponse;
import com.jitong.projectflow.common.web.TraceIdFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class SecurityErrorResponseWriter {
    private final ObjectMapper objectMapper;

    public void write(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), ApiResponse.failure(status, message, traceId()));
    }

    private String traceId() {
        String traceId = MDC.get(TraceIdFilter.TRACE_ID);
        return traceId == null ? "" : traceId;
    }
}
