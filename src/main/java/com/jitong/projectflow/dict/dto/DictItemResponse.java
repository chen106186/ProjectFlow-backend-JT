package com.jitong.projectflow.dict.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "字典项响应。")
public record DictItemResponse(
        @Schema(description = "字典值，前端提交接口时使用。")
        String value,

        @Schema(description = "中文展示名称。")
        String label,

        @Schema(description = "排序号，数值越小越靠前。")
        int sortOrder) {
}
