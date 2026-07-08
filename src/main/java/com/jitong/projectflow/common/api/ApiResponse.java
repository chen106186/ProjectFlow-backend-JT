package com.jitong.projectflow.common.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "统一接口响应结构。")
public record ApiResponse<T>(
        @Schema(description = "业务状态码，0表示成功，非0表示失败。")
        int code,

        @Schema(description = "响应消息。失败时该字段可直接用于前端回显。")
        String message,

        @Schema(description = "响应数据。")
        T data,

        @Schema(description = "请求链路追踪ID，便于根据前端回显定位后端日志。")
        String traceId) {
    public static <T> ApiResponse<T> success(T data, String traceId) {
        return new ApiResponse<>(0, "success", data, traceId);
    }

    public static <T> ApiResponse<T> failure(int code, String message, String traceId) {
        return new ApiResponse<>(code, message, null, traceId);
    }
}
