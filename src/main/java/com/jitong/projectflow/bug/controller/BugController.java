package com.jitong.projectflow.bug.controller;

import com.jitong.projectflow.common.api.ApiResponse;
import com.jitong.projectflow.common.api.PageResult;
import com.jitong.projectflow.bug.dto.BugAssignRequest;
import com.jitong.projectflow.bug.dto.BugCommentCreateRequest;
import com.jitong.projectflow.bug.dto.BugCommentResponse;
import com.jitong.projectflow.bug.dto.BugCreateRequest;
import com.jitong.projectflow.bug.dto.BugFixRequest;
import com.jitong.projectflow.bug.dto.BugResolveRequest;
import com.jitong.projectflow.bug.dto.BugQueryRequest;
import com.jitong.projectflow.bug.dto.BugResponse;
import com.jitong.projectflow.bug.dto.BugSummaryResponse;
import com.jitong.projectflow.bug.dto.BugUpdateRequest;
import com.jitong.projectflow.bug.service.BugService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/bugs")
@RequiredArgsConstructor
@Tag(name = "BUG管理", description = "Bug 提交、查询、指派、关闭和评论接口")
public class BugController {
    private final BugService bugService;

    @Operation(summary = "新增 Bug",
            description = "提交缺陷信息，指定处理人，并向处理人生成待处理通知。")
    @PostMapping
    public ApiResponse<BugResponse> create(@Valid @RequestBody BugCreateRequest request) {
        return ApiResponse.success(bugService.create(request), MDC.get("traceId"));
    }

    @Operation(summary = "分页查询 Bug 列表",
            description = "按项目、状态、优先级和关键字查询缺陷列表。")
    @GetMapping
    public ApiResponse<PageResult<BugResponse>> listBugs(@Valid @ModelAttribute BugQueryRequest request) {
        return ApiResponse.success(bugService.list(request), MDC.get("traceId"));
    }

//    @Operation(summary = "查询我的 Bug",
//            description = "查询当前登录用户创建或被指定处理的 Bug。")
    @Operation(summary = "查询 Bug 全量统计",
            description = "按项目范围统计 Bug 总数、状态分布和优先级分布；执行类项目会兼容统计其关联管理类项目下的 Bug。")
    @GetMapping("/summary")
    public ApiResponse<BugSummaryResponse> summary(@RequestParam(required = false) Long projectId) {
        return ApiResponse.success(bugService.summary(projectId), MDC.get("traceId"));
    }

    @Operation(summary = "查询我的 Bug",
            description = "查询当前登录用户创建或被指定处理的全部 Bug，支持按关键字、项目、优先级、状态和创建人筛选。")
    @GetMapping("/my")
    public ApiResponse<List<BugResponse>> listMine(@ModelAttribute BugQueryRequest request) {
        return ApiResponse.success(bugService.listMine(request), MDC.get("traceId"));
    }

    @Operation(summary = "查询 Bug 详情",
            description = "根据 Bug ID 查询缺陷标题、状态、优先级、创建人、指定人和复现步骤。")
    @GetMapping("/{id:\\d+}")
    public ApiResponse<BugResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(bugService.getById(id), MDC.get("traceId"));
    }

    @Operation(summary = "编辑 Bug",
            description = "更新缺陷基础信息，只有创建人、指定人或系统管理员可操作。")
    @PutMapping("/{id:\\d+}")
    public ApiResponse<BugResponse> update(@PathVariable Long id, @Valid @RequestBody BugUpdateRequest request) {
        return ApiResponse.success(bugService.update(id, request), MDC.get("traceId"));
    }

    @Operation(summary = "指派 Bug",
            description = "更换 Bug 指定处理人，记录指派原因并向新处理人发送通知。")
    @PatchMapping("/{id:\\d+}/assign")
    public ApiResponse<BugResponse> assign(@PathVariable Long id, @Valid @RequestBody BugAssignRequest request) {
        return ApiResponse.success(bugService.assign(id, request), MDC.get("traceId"));
    }

    @Operation(summary = "关闭 Bug",
            description = "关闭指定 Bug，只有创建人或系统管理员可执行关闭操作。")
    @PatchMapping("/{id:\\d+}/close")
    public ApiResponse<BugResponse> close(@PathVariable Long id) {
        return ApiResponse.success(bugService.close(id), MDC.get("traceId"));
    }

    @Operation(summary = "重新打开 Bug",
            description = "将已关闭的 Bug 状态重置为待修复，只有创建人或系统管理员可操作。")
    @PatchMapping("/{id:\\d+}/reopen")
    public ApiResponse<BugResponse> reopen(@PathVariable Long id) {
        return ApiResponse.success(bugService.reopen(id), MDC.get("traceId"));
    }

    @Operation(summary = "删除 Bug",
            description = "逻辑删除指定 Bug，只有创建人或系统管理员可操作。")
    @DeleteMapping("/{id:\\d+}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        bugService.delete(id);
        return ApiResponse.success(null, MDC.get("traceId"));
    }

    @Operation(summary = "提交修复详情",
            description = "负责人填写问题分析和修复细节，Bug 状态自动变更为待验证。")
    @PostMapping("/{id:\\d+}/fix")
    public ApiResponse<BugResponse> fix(@PathVariable Long id, @RequestBody BugFixRequest request) {
        return ApiResponse.success(bugService.fix(id, request), MDC.get("traceId"));
    }

    @Operation(summary = "解决 Bug",
            description = "填写解决方案、解决日期和备注，Bug 状态变更为待验证，并通知创建人验证。")
    @PostMapping("/{id:\\d+}/resolve")
    public ApiResponse<BugResponse> resolve(@PathVariable Long id, @Valid @RequestBody BugResolveRequest request) {
        return ApiResponse.success(bugService.resolve(id, request), MDC.get("traceId"));
    }

    @Operation(summary = "新增 Bug 评论",
            description = "在 Bug 详情中追加评论，并向指定处理人发送评论通知。")
    @PostMapping("/{id:\\d+}/comments")
    public ApiResponse<BugCommentResponse> addComment(@PathVariable Long id, @Valid @RequestBody BugCommentCreateRequest request) {
        return ApiResponse.success(bugService.addComment(id, request), MDC.get("traceId"));
    }

    @Operation(summary = "查询 Bug 评论列表",
            description = "按时间倒序查询指定 Bug 下的全部评论记录。")
    @GetMapping("/{id:\\d+}/comments")
    public ApiResponse<List<BugCommentResponse>> listComments(@PathVariable Long id) {
        return ApiResponse.success(bugService.listComments(id), MDC.get("traceId"));
    }
}
