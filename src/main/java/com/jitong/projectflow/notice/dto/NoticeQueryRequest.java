package com.jitong.projectflow.notice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.jitong.projectflow.common.api.PageQuery;
import lombok.Data;

@Data
public class NoticeQueryRequest extends PageQuery {
    @Schema(description = "是否已读。")
    private Boolean read;
    @Schema(description = "通知类型。")
    private String noticeType;
    @Schema(description = "业务类型，例如 PROJECT、TASK、BUG、REQUIREMENT。")
    private String businessType;
    @Schema(description = "业务数据 ID。")
    private Long businessId;
    @Schema(description = "关键字，模糊匹配通知标题或内容。")
    private String keyword;
}
