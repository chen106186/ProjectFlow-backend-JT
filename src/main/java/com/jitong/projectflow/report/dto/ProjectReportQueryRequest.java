package com.jitong.projectflow.report.dto;

import com.jitong.projectflow.common.api.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
public class ProjectReportQueryRequest extends PageQuery {
    private Long projectId;
    private String status;
    private String reportType;
    private LocalDate plannedDateFrom;
    private LocalDate plannedDateTo;
    private String keyword;
}
