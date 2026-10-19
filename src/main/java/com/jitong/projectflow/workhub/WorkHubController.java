package com.jitong.projectflow.workhub;

import com.jitong.projectflow.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/workhub")
@RequiredArgsConstructor
@Tag(name = "交付与协作", description = "需求交付追溯、项目工作台、交付版本、评论、阻塞和个人视图")
public class WorkHubController {
    private final WorkHubService workHub;

    @GetMapping("/requirements/{id}/delivery")
    @Operation(summary = "查询需求关联任务与验收结论")
    public ApiResponse<Map<String, Object>> delivery(@PathVariable Long id) {
        return ok(workHub.requirementDelivery(id));
    }

    @PutMapping("/requirements/{id}/tasks")
    @Operation(summary = "设置需求关联任务，同项目校验并重置已有验收")
    public ApiResponse<Map<String, Object>> linkTasks(@PathVariable Long id, @Valid @RequestBody WorkHubRequests.TaskLinks request) {
        return ok(workHub.linkRequirementTasks(id, request));
    }

    @PutMapping("/requirements/{id}/acceptance")
    @Operation(summary = "记录需求验收结论；未完成任务或未关闭关联 Bug 不可通过")
    public ApiResponse<Map<String, Object>> accept(@PathVariable Long id, @Valid @RequestBody WorkHubRequests.Acceptance request) {
        return ok(workHub.acceptRequirement(id, request));
    }

    @GetMapping("/projects/{id}/overview")
    @Operation(summary = "项目工作台，汇总需求、任务、Bug、文件、交付版本和最近动态")
    public ApiResponse<Map<String, Object>> overview(@PathVariable Long id) {
        return ok(workHub.projectOverview(id));
    }

    @GetMapping("/projects/{id}/releases")
    @Operation(summary = "查询项目交付版本和实时就绪状态")
    public ApiResponse<List<Map<String, Object>>> releases(@PathVariable Long id) {
        return ok(workHub.releases(id));
    }

    @PostMapping("/releases")
    @Operation(summary = "创建交付版本")
    public ApiResponse<Map<String, Object>> createRelease(@Valid @RequestBody WorkHubRequests.Release request) {
        return ok(workHub.saveRelease(null, request));
    }

    @PutMapping("/releases/{id}")
    @Operation(summary = "更新交付版本；就绪和发布受验收清单约束")
    public ApiResponse<Map<String, Object>> updateRelease(@PathVariable String id, @Valid @RequestBody WorkHubRequests.Release request) {
        return ok(workHub.saveRelease(id, request));
    }

    @GetMapping("/tasks/{id}/blocker")
    @Operation(summary = "查询任务阻塞原因")
    public ApiResponse<Map<String, Object>> blocker(@PathVariable Long id) {
        return ok(workHub.taskBlocker(id));
    }

    @GetMapping("/tasks/{id}/requirements")
    @Operation(summary = "查询任务关联需求与验收状态")
    public ApiResponse<List<Map<String, Object>>> taskRequirements(@PathVariable Long id) {
        return ok(workHub.taskRequirements(id));
    }

    @PutMapping("/tasks/{id}/blocker")
    @Operation(summary = "登记或清除任务阻塞原因")
    public ApiResponse<Map<String, Object>> updateBlocker(@PathVariable Long id, @Valid @RequestBody WorkHubRequests.Blocker request) {
        return ok(workHub.updateTaskBlocker(id, request));
    }

    @GetMapping("/{type:tasks|requirements}/{id}/comments")
    @Operation(summary = "查询任务或需求评论")
    public ApiResponse<List<Map<String, Object>>> comments(@PathVariable String type, @PathVariable Long id) {
        return ok(workHub.comments(type, id));
    }

    @GetMapping("/{type:tasks|requirements}/{id}/mention-candidates")
    @Operation(summary = "查询可在该任务或需求中提及的项目成员")
    public ApiResponse<List<Map<String, Object>>> mentionCandidates(@PathVariable String type, @PathVariable Long id) {
        return ok(workHub.mentionCandidates(type, id));
    }

    @PostMapping("/{type:tasks|requirements}/{id}/comments")
    @Operation(summary = "发表任务或需求评论并通知被提及的项目成员")
    public ApiResponse<List<Map<String, Object>>> comment(@PathVariable String type, @PathVariable Long id,
                                                            @Valid @RequestBody WorkHubRequests.Comment request) {
        return ok(workHub.addComment(type, id, request));
    }

    @GetMapping("/search")
    @Operation(summary = "按标题或编号快速查找本人可见的需求、任务和 Bug")
    public ApiResponse<List<Map<String, Object>>> search(@RequestParam String q) {
        return ok(workHub.search(q));
    }

    @GetMapping("/saved-views")
    @Operation(summary = "查询本人保存的筛选视图")
    public ApiResponse<List<Map<String, Object>>> savedViews(@RequestParam(required = false) String scope) {
        return ok(workHub.savedViews(scope));
    }

    @PostMapping("/saved-views")
    @Operation(summary = "保存个人常用筛选")
    public ApiResponse<List<Map<String, Object>>> saveView(@Valid @RequestBody WorkHubRequests.SavedView request) {
        return ok(workHub.saveView(request));
    }

    @DeleteMapping("/saved-views/{id}")
    @Operation(summary = "删除本人保存的筛选视图")
    public ApiResponse<Void> deleteView(@PathVariable String id) {
        workHub.deleteView(id);
        return ok(null);
    }

    private <T> ApiResponse<T> ok(T value) {
        return ApiResponse.success(value, MDC.get("traceId"));
    }
}
