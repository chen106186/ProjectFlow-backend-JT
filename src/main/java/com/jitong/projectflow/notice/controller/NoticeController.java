package com.jitong.projectflow.notice.controller;

import com.jitong.projectflow.auth.security.CurrentUserContext;
import com.jitong.projectflow.common.api.ApiResponse;
import com.jitong.projectflow.notice.dto.NoticeResponse;
import com.jitong.projectflow.notice.service.NoticeService;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notices")
public class NoticeController {

    private final NoticeService noticeService;

    public NoticeController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @GetMapping
    public ApiResponse<List<NoticeResponse>> list() {
        Long userId = CurrentUserContext.userId();
        return ApiResponse.success(noticeService.list(userId), MDC.get("traceId"));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Long> unreadCount() {
        Long userId = CurrentUserContext.userId();
        return ApiResponse.success(noticeService.unreadCount(userId), MDC.get("traceId"));
    }

    @PatchMapping("/read-all")
    public ApiResponse<Void> markAllRead() {
        Long userId = CurrentUserContext.userId();
        noticeService.markAllRead(userId);
        return ApiResponse.success(null, MDC.get("traceId"));
    }

    @PatchMapping("/{id}/read")
    public ApiResponse<Void> markRead(@PathVariable Long id) {
        Long userId = CurrentUserContext.userId();
        noticeService.markRead(userId, id);
        return ApiResponse.success(null, MDC.get("traceId"));
    }
}
