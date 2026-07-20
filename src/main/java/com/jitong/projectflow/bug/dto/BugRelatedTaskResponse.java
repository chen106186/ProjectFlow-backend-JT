package com.jitong.projectflow.bug.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Bug 关联任务。")
public class BugRelatedTaskResponse {
    @Schema(description = "任务 ID。")
    private Long id;

    @Schema(description = "任务名称。")
    private String name;
}
