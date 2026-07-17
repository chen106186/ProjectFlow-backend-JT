package com.jitong.projectflow.bug.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class BugResolveRequest {

    @NotBlank(message = "请选择解决方案")
    private String solution;

    @NotNull(message = "请选择解决日期")
    private LocalDate resolvedDate;

    /** 可选：解决时变更指派人 */
    private Long assigneeId;

    /** 可选：解决备注（富文本 HTML） */
    private String remark;
}
