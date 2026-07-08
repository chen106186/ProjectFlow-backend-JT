package com.jitong.projectflow.common.error;

import com.jitong.projectflow.common.api.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {
    @Test
    void wrapsBusinessExceptionWithConfiguredErrorCode() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        ResponseEntity<ApiResponse<Void>> response = handler.handleBusiness(
                new BusinessException(ErrorCode.NOT_FOUND, "任务不存在"),
                request("/api/tasks/1"));

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(404);
        assertThat(response.getBody().message()).isEqualTo("任务不存在");
    }

    @Test
    void wrapsDataAccessExceptionWithFrontendMessage() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        ResponseEntity<ApiResponse<Void>> response = handler.handleDataAccess(
                new DataIntegrityViolationException("project_id cannot be null"),
                request("/api/daily-reports"));

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(500);
        assertThat(response.getBody().message()).isEqualTo("数据保存或查询失败，请检查提交内容后重试。");
    }

    private MockHttpServletRequest request(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(uri);
        return request;
    }
}
