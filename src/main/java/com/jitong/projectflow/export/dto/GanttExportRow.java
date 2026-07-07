package com.jitong.projectflow.export.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.time.LocalDate;

@Data
public class GanttExportRow {
    @ExcelProperty("ID") private Long id;
    @ExcelProperty("项目ID") private Long projectId;
    @ExcelProperty("节点名称") private String nodeName;
    @ExcelProperty("节点类型") private String nodeType;
    @ExcelProperty("状态") private String status;
    @ExcelProperty("进度(%)") private Integer progressPercent;
    @ExcelProperty("计划开始") private LocalDate plannedStartDate;
    @ExcelProperty("计划结束") private LocalDate plannedEndDate;
    @ExcelProperty("实际开始") private LocalDate actualStartDate;
    @ExcelProperty("实际结束") private LocalDate actualEndDate;
}
