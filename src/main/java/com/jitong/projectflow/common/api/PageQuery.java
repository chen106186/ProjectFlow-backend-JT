package com.jitong.projectflow.common.api;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
@Schema(description = "分页查询参数。")
public class PageQuery {
    @Min(1)
    @Schema(description = "页码，从 1 开始。")
    private Long pageNo = 1L;

    @Min(1)
    @Max(200)
    @Schema(description = "每页条数，最大 200。")
    private Long pageSize = 20L;
}
