package com.jitong.projectflow.daily.dto;

import com.jitong.projectflow.common.api.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
public class DailyReportQueryRequest extends PageQuery {
    private Long projectId;
    private Long reporterId;
    private LocalDate dateFrom;
    private LocalDate dateTo;
    private String keyword;
}
