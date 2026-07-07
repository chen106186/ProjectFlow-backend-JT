package com.jitong.projectflow.task.dto;

import com.jitong.projectflow.common.api.PageQuery;
import lombok.Data;

import java.time.LocalDate;

@Data
public class TaskQueryRequest extends PageQuery {
    private Long projectId;
    private Long assigneeId;
    private String priority;
    private String status;
    private String keyword;
    private LocalDate plannedEndDate;
}
