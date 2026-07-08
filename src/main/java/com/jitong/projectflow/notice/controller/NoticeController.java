package com.jitong.projectflow.notice.controller;

import com.jitong.projectflow.auth.security.CurrentUserContext;
import com.jitong.projectflow.common.api.ApiResponse;
import com.jitong.projectflow.common.api.PageResult;
import com.jitong.projectflow.notice.dto.NoticeQueryRequest;
import com.jitong.projectflow.notice.dto.NoticeResponse;
import com.jitong.projectflow.notice.service.NoticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notices")
@Tag(name = "通知中心", description = "个人通知查询、未读统计和已读处理接口")
public class NoticeController {

    private final NoticeService noticeService;

    public NoticeController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @Operation(summary = "分页查询通知列表",
            description = "分页查询当前登录用户的通知，可按类型、已读状态和关键字筛选。")
    @GetMapping
    public ApiResponse<PageResult<NoticeResponse>> list(@Valid @ModelAttribute NoticeQueryRequest request) {
        Long userId = CurrentUserContext.userId();
        return ApiResponse.success(noticeService.list(userId, request), MDC.get("traceId"));
    }

    @Operation(summary = "查询未读通知数量",
            description = "统计当前登录用户尚未阅读的通知数量。")
    @GetMapping("/unread-count")
    public ApiResponse<Long> unreadCount() {
        Long userId = CurrentUserContext.userId();
        return ApiResponse.success(noticeService.unreadCount(userId), MDC.get("traceId"));
    }

    @Operation(summary = "全部通知标记为已读",
            description = "将当前登录用户的全部未读通知一次性标记为已读。")
    @PatchMapping("/read-all")
    public ApiResponse<Void> markAllRead() {
        Long userId = CurrentUserContext.userId();
        noticeService.markAllRead(userId);
        return ApiResponse.success(null, MDC.get("traceId"));
    }

    @Operation(summary = "单条通知标记为已读",
            description = "将当前登录用户的一条指定通知标记为已读。")
    @PatchMapping("/{id}/read")
    public ApiResponse<Void> markRead(@PathVariable Long id) {
        Long userId = CurrentUserContext.userId();
        noticeService.markRead(userId, id);
        return ApiResponse.success(null, MDC.get("traceId"));
    }
}
