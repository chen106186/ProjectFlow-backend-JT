package com.jitong.projectflow.export.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.time.LocalDate;

@Data
public class TaskExportRow {
    @ExcelProperty("ID") private Long id;
    @ExcelProperty("项目ID") private Long projectId;
    @ExcelProperty("任务名称") private String name;
    @ExcelProperty("优先级") private String priority;
    @ExcelProperty("状态") private String status;
    @ExcelProperty("负责人ID") private Long assigneeId;
    @ExcelProperty("计划开始日期") private LocalDate plannedStartDate;
    @ExcelProperty("计划结束日期") private LocalDate plannedEndDate;
    @ExcelProperty("实际开始日期") private LocalDate actualStartDate;
    @ExcelProperty("实际结束日期") private LocalDate actualEndDate;
}
