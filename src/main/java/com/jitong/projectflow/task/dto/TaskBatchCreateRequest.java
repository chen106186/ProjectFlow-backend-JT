package com.jitong.projectflow.task.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "批量创建任务请求。")
public class TaskBatchCreateRequest {
    @Valid
    @NotEmpty
    @Schema(description = "任务列表。")
    private List<TaskCreateRequest> tasks = new ArrayList<>();
}
