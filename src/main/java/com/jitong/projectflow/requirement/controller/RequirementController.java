package com.jitong.projectflow.requirement.controller;

import com.jitong.projectflow.common.api.ApiResponse;
import com.jitong.projectflow.requirement.dto.RequirementCreateRequest;
import com.jitong.projectflow.requirement.dto.RequirementResponse;
import com.jitong.projectflow.requirement.dto.RequirementStatusUpdateRequest;
import com.jitong.projectflow.requirement.dto.RequirementUpdateRequest;
import com.jitong.projectflow.requirement.service.RequirementService;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/requirements")
public class RequirementController {

    private final RequirementService requirementService;

    public RequirementController(RequirementService requirementService) {
        this.requirementService = requirementService;
    }

    @PostMapping
    public ApiResponse<RequirementResponse> create(@Valid @RequestBody RequirementCreateRequest req) {
        return ApiResponse.success(requirementService.create(req), MDC.get("traceId"));
    }

    @GetMapping
    public ApiResponse<List<RequirementResponse>> list(@RequestParam(required = false) Long projectId) {
        return ApiResponse.success(requirementService.list(projectId), MDC.get("traceId"));
    }

    @GetMapping("/{id}")
    public ApiResponse<RequirementResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(requirementService.getById(id), MDC.get("traceId"));
    }

    @PutMapping("/{id}")
    public ApiResponse<RequirementResponse> update(@PathVariable Long id,
                                                    @Valid @RequestBody RequirementUpdateRequest req) {
        return ApiResponse.success(requirementService.update(id, req), MDC.get("traceId"));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<RequirementResponse> updateStatus(@PathVariable Long id,
                                                          @Valid @RequestBody RequirementStatusUpdateRequest req) {
        return ApiResponse.success(requirementService.updateStatus(id, req), MDC.get("traceId"));
    }

    @GetMapping("/my")
    public ApiResponse<List<RequirementResponse>> listMine() {
        return ApiResponse.success(requirementService.listMine(), MDC.get("traceId"));
    }
}
