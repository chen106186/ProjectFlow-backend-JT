package com.jitong.projectflow.bug.controller;

import com.jitong.projectflow.common.api.ApiResponse;
import com.jitong.projectflow.common.api.PageResult;
import com.jitong.projectflow.bug.dto.BugAssignRequest;
import com.jitong.projectflow.bug.dto.BugCommentCreateRequest;
import com.jitong.projectflow.bug.dto.BugCommentResponse;
import com.jitong.projectflow.bug.dto.BugCreateRequest;
import com.jitong.projectflow.bug.dto.BugQueryRequest;
import com.jitong.projectflow.bug.dto.BugResponse;
import com.jitong.projectflow.bug.dto.BugUpdateRequest;
import com.jitong.projectflow.bug.service.BugService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/bugs")
@RequiredArgsConstructor
public class BugController {
    private final BugService bugService;

    @PostMapping
    public ApiResponse<BugResponse> create(@Valid @RequestBody BugCreateRequest request) {
        return ApiResponse.success(bugService.create(request), MDC.get("traceId"));
    }

    @GetMapping
    public ApiResponse<PageResult<BugResponse>> listBugs(@Valid @ModelAttribute BugQueryRequest request) {
        return ApiResponse.success(bugService.list(request), MDC.get("traceId"));
    }

    @GetMapping("/my")
    public ApiResponse<List<BugResponse>> listMine() {
        return ApiResponse.success(bugService.listMine(), MDC.get("traceId"));
    }

    @GetMapping("/{id}")
    public ApiResponse<BugResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(bugService.getById(id), MDC.get("traceId"));
    }

    @PutMapping("/{id}")
    public ApiResponse<BugResponse> update(@PathVariable Long id, @Valid @RequestBody BugUpdateRequest request) {
        return ApiResponse.success(bugService.update(id, request), MDC.get("traceId"));
    }

    @PatchMapping("/{id}/assign")
    public ApiResponse<BugResponse> assign(@PathVariable Long id, @Valid @RequestBody BugAssignRequest request) {
        return ApiResponse.success(bugService.assign(id, request), MDC.get("traceId"));
    }

    @PatchMapping("/{id}/close")
    public ApiResponse<BugResponse> close(@PathVariable Long id) {
        return ApiResponse.success(bugService.close(id), MDC.get("traceId"));
    }

    @PostMapping("/{id}/comments")
    public ApiResponse<BugCommentResponse> addComment(@PathVariable Long id, @Valid @RequestBody BugCommentCreateRequest request) {
        return ApiResponse.success(bugService.addComment(id, request), MDC.get("traceId"));
    }

    @GetMapping("/{id}/comments")
    public ApiResponse<List<BugCommentResponse>> listComments(@PathVariable Long id) {
        return ApiResponse.success(bugService.listComments(id), MDC.get("traceId"));
    }
}
