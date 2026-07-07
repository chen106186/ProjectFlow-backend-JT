package com.jitong.projectflow.notice.dto;

import com.jitong.projectflow.common.api.PageQuery;
import lombok.Data;

@Data
public class NoticeQueryRequest extends PageQuery {
    private Boolean read;
    private String noticeType;
    private String businessType;
    private Long businessId;
}
