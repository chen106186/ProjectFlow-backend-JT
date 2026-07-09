package com.jitong.projectflow.common.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {
    @Test
    void successUsesChineseDefaultMessage() {
        ApiResponse<String> response = ApiResponse.success("ok", "trace-1");

        assertThat(response.code()).isEqualTo(0);
        assertThat(response.message()).isEqualTo("操作成功");
        assertThat(response.data()).isEqualTo("ok");
        assertThat(response.traceId()).isEqualTo("trace-1");
    }

    @Test
    void successAllowsCustomMessage() {
        ApiResponse<String> response = ApiResponse.success("保存成功", "ok", "trace-2");

        assertThat(response.code()).isEqualTo(0);
        assertThat(response.message()).isEqualTo("保存成功");
        assertThat(response.data()).isEqualTo("ok");
        assertThat(response.traceId()).isEqualTo("trace-2");
    }
}
