package com.jitong.projectflow.common.error;

import com.jitong.projectflow.common.api.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {
    @Test
    void wrapsBusinessExceptionWithConfiguredErrorCode() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        ResponseEntity<ApiResponse<Void>> response = handler.handleBusiness(
                new BusinessException(ErrorCode.NOT_FOUND, "task not found"));

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(404);
        assertThat(response.getBody().message()).isEqualTo("task not found");
    }
}
