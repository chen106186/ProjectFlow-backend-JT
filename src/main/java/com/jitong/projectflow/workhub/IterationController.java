package com.jitong.projectflow.workhub;

import com.jitong.projectflow.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/workhub/releases/{releaseId}/workflow")
@RequiredArgsConstructor
@Tag(name = "版本迭代档案", description = "阶段责任、过程记录、上线申请和审批留痕")
public class IterationController {
    private final IterationService iterations;

    @GetMapping
    @Operation(summary = "查询版本迭代档案")
    public ApiResponse<Map<String, Object>> workflow(@PathVariable String releaseId) {
        return ok(iterations.workflow(releaseId));
    }

    @PutMapping("/stages/{code}")
    @Operation(summary = "更新阶段计划、责任人与结论并记录变更前后内容")
    public ApiResponse<Map<String, Object>> stage(@PathVariable String releaseId, @PathVariable String code,
                                                  @Valid @RequestBody IterationRequests.StageUpdate request) {
        return ok(iterations.updateStage(releaseId, code, request));
    }

    @PostMapping("/records")
    @Operation(summary = "追加不可覆盖的迭代记录或凭证链接")
    public ApiResponse<Map<String, Object>> record(@PathVariable String releaseId,
                                                    @Valid @RequestBody IterationRequests.Record request) {
        return ok(iterations.addRecord(releaseId, request));
    }

    @PutMapping("/application")
    @Operation(summary = "填写上线申请单")
    public ApiResponse<Map<String, Object>> application(@PathVariable String releaseId,
                                                         @Valid @RequestBody IterationRequests.Application request) {
        return ok(iterations.saveApplication(releaseId, request));
    }

    @PostMapping("/application/submit")
    @Operation(summary = "完成阶段与交付检查后提交上线申请")
    public ApiResponse<Map<String, Object>> submit(@PathVariable String releaseId) {
        return ok(iterations.submitApplication(releaseId));
    }

    @PostMapping("/application/decision")
    @Operation(summary = "指定审批人审批上线申请")
    public ApiResponse<Map<String, Object>> decision(@PathVariable String releaseId,
                                                      @Valid @RequestBody IterationRequests.Decision request) {
        return ok(iterations.decideApplication(releaseId, request));
    }

    @PostMapping("/application/withdraw")
    @Operation(summary = "撤回审批中或已批准的上线申请并记录原因")
    public ApiResponse<Map<String, Object>> withdraw(@PathVariable String releaseId,
                                                     @Valid @RequestBody IterationRequests.Withdrawal request) {
        return ok(iterations.withdrawApplication(releaseId, request));
    }

    private ApiResponse<Map<String, Object>> ok(Map<String, Object> value) {
        return ApiResponse.success(value, MDC.get("traceId"));
    }
}
