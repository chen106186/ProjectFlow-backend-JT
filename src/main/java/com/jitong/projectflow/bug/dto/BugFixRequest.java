package com.jitong.projectflow.bug.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "填写修复详情请求。")
public class BugFixRequest {
    @Schema(description = "问题分析。")
    private String fixAnalysis;
    @Schema(description = "修复细节。")
    private String fixDetail;
}
