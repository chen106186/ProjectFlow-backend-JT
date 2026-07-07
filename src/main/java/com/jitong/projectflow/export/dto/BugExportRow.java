package com.jitong.projectflow.export.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BugExportRow {
    @ExcelProperty("ID") private Long id;
    @ExcelProperty("项目ID") private Long projectId;
    @ExcelProperty("标题") private String title;
    @ExcelProperty("状态") private String status;
    @ExcelProperty("优先级") private String priority;
    @ExcelProperty("创建人ID") private Long creatorId;
    @ExcelProperty("负责人ID") private Long assigneeId;
    @ExcelProperty("创建时间") private LocalDateTime createdAt;
}
