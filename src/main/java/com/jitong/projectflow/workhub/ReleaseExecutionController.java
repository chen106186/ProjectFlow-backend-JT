package com.jitong.projectflow.workhub;

import com.jitong.projectflow.common.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/workhub/tasks/{taskId}/release-execution")
@RequiredArgsConstructor
public class ReleaseExecutionController {
    private final ReleaseExecutionService executions;

    @GetMapping
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long taskId) {
        return ApiResponse.success(executions.detail(taskId), MDC.get("traceId"));
    }

    @PutMapping("/plan")
    public ApiResponse<Map<String, Object>> plan(@PathVariable Long taskId,
            @Valid @RequestBody ReleaseExecutionRequests.Plan request) {
        return ApiResponse.success(executions.updatePlan(taskId, request), MDC.get("traceId"));
    }

    @PostMapping("/meetings")
    public ApiResponse<Map<String, Object>> meeting(@PathVariable Long taskId,
            @Valid @RequestBody ReleaseExecutionRequests.Meeting request) {
        return ApiResponse.success(executions.addMeeting(taskId, request), MDC.get("traceId"));
    }

    @PostMapping("/meetings/{meetingId}/confirm")
    public ApiResponse<Map<String, Object>> confirmMeeting(@PathVariable Long taskId,
            @PathVariable String meetingId) {
        return ApiResponse.success(executions.confirmMeeting(taskId, meetingId), MDC.get("traceId"));
    }

    @PostMapping("/report")
    public ApiResponse<Map<String, Object>> report(@PathVariable Long taskId,
            @Valid @RequestBody ReleaseExecutionRequests.Report request) {
        return ApiResponse.success(executions.submitReport(taskId, request), MDC.get("traceId"));
    }

    @PostMapping("/report/verify")
    public ApiResponse<Map<String, Object>> verifyReport(@PathVariable Long taskId,
            @Valid @RequestBody ReleaseExecutionRequests.Verification request) {
        return ApiResponse.success(executions.verifyReport(taskId, request), MDC.get("traceId"));
    }
}
