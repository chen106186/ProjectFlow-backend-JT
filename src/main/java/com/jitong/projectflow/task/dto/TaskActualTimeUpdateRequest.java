package com.jitong.projectflow.task.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDate;

@Data
public class TaskActualTimeUpdateRequest {
    @Schema(description = "实际开始日期，格式 yyyy-MM-dd。")
    private LocalDate actualStartDate;
    @Schema(description = "实际结束日期，格式 yyyy-MM-dd。")
    private LocalDate actualEndDate;
    @Schema(description = "备注。")
    private String remark;

    @JsonIgnore
    private boolean actualStartDatePresent;
    @JsonIgnore
    private boolean actualEndDatePresent;

    public void setActualStartDate(LocalDate date) {
        this.actualStartDate = date;
        this.actualStartDatePresent = true;
    }

    public void setActualEndDate(LocalDate date) {
        this.actualEndDate = date;
        this.actualEndDatePresent = true;
    }
}
