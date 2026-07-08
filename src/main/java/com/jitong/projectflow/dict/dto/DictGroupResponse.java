package com.jitong.projectflow.dict.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "字典分组响应。")
public record DictGroupResponse(
        @Schema(description = "字典类型编码。")
        String type,

        @Schema(description = "字典类型名称。")
        String name,

        @Schema(description = "字典项列表。")
        List<DictItemResponse> items) {
}
