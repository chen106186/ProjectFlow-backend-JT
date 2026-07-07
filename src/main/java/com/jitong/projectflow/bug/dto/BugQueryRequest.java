package com.jitong.projectflow.bug.dto;

import com.jitong.projectflow.common.api.PageQuery;
import lombok.Data;

@Data
public class BugQueryRequest extends PageQuery {
    private String status;
    private String priority;
    private Long projectId;
    private String keyword;
}
