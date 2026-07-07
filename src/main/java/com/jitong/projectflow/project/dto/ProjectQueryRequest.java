package com.jitong.projectflow.project.dto;

import com.jitong.projectflow.common.api.PageQuery;
import lombok.Data;

@Data
public class ProjectQueryRequest extends PageQuery {
    private String projectType;
    private String status;
    private String contractStatus;
    private Long managerId;
    private String keyword;
}
