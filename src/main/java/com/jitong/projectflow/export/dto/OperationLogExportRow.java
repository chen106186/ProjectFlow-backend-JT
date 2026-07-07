package com.jitong.projectflow.export.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OperationLogExportRow {
    @ExcelProperty("ID") private Long id;
    @ExcelProperty("模块") private String module;
    @ExcelProperty("业务类型") private String businessType;
    @ExcelProperty("业务ID") private Long businessId;
    @ExcelProperty("操作类型") private String operationType;
    @ExcelProperty("操作人ID") private Long operatorId;
    @ExcelProperty("内容") private String content;
    @ExcelProperty("创建时间") private LocalDateTime createdAt;
}
