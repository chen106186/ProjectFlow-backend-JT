package com.jitong.projectflow.common.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "分页查询结果。")
public record PageResult<T>(
        @Schema(description = "总记录数。")
        long total,

        @Schema(description = "当前页码，从 1 开始。")
        long pageNo,

        @Schema(description = "每页条数。")
        long pageSize,

        @Schema(description = "当前页数据列表。")
        List<T> records) {
}
