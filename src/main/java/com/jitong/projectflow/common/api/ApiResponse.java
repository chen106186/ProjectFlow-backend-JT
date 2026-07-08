package com.jitong.projectflow.common.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "统一接口响应结构。")
public record ApiResponse<T>(
        @Schema(description = "业务状态码，0 表示成功，非 0 表示失败。")
        int code,

        @Schema(description = "响应消息，前端可直接用于提示展示。")
        String message,

        @Schema(description = "响应数据。")
        T data,

        @Schema(description = "请求追踪 ID，便于根据前端提示定位后端日志。")
        String traceId) {

    public static <T> ApiResponse<T> success(T data, String traceId) {
        return new ApiResponse<>(0, "操作成功", data, traceId);
    }

    public static <T> ApiResponse<T> success(String message, T data, String traceId) {
        return new ApiResponse<>(0, message, data, traceId);
    }

    public static <T> ApiResponse<T> failure(int code, String message, String traceId) {
        return new ApiResponse<>(code, message, null, traceId);
    }
}
