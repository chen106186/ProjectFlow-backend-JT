package com.jitong.projectflow.task.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.time.LocalDate;

@Data
public class TaskImportRow {
    @ExcelProperty("项目ID")
    private Long projectId;

    @ExcelProperty("父任务ID")
    private Long parentId;

    @ExcelProperty("任务名称")
    private String name;

    @ExcelProperty("角色")
    private String roleName;

    @ExcelProperty("优先级")
    private String priority;

    @ExcelProperty("负责人ID")
    private Long assigneeId;

    @ExcelProperty("计划开始")
    private LocalDate plannedStartDate;

    @ExcelProperty("计划结束")
    private LocalDate plannedEndDate;

    @ExcelProperty("描述")
    private String description;

    @ExcelProperty("标签")
    private String tags;

    @ExcelProperty("备注")
    private String remark;

    @ExcelProperty("排序")
    private Integer sortOrder;
}
