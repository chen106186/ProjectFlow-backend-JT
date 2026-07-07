package com.jitong.projectflow.project.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class GanttNodeResponse {

    private Long id;

    private Long projectId;

    private Long parentId;

    private String nodeName;

    private String nodeType;

    private LocalDate plannedStartDate;

    private LocalDate plannedEndDate;

    private LocalDate actualStartDate;

    private LocalDate actualEndDate;

    private String status;

    private Integer progressPercent;

    private Integer sortOrder;
}
