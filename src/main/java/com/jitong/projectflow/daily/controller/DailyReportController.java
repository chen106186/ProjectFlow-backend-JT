package com.jitong.projectflow.daily.controller;

import com.jitong.projectflow.common.api.ApiResponse;
import com.jitong.projectflow.common.api.PageResult;
import com.jitong.projectflow.daily.dto.DailyReportCreateRequest;
import com.jitong.projectflow.daily.dto.DailyReportQueryRequest;
import com.jitong.projectflow.daily.dto.DailyReportResponse;
import com.jitong.projectflow.daily.dto.DailyReportUpdateRequest;
import com.jitong.projectflow.daily.service.DailyReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/daily-reports")
@RequiredArgsConstructor
public class DailyReportController {
    private final DailyReportService dailyReportService;

    @PostMapping
    @PreAuthorize("hasAuthority('daily-report:create')")
    public ApiResponse<DailyReportResponse> create(@Valid @RequestBody DailyReportCreateRequest request) {
        return ApiResponse.success(dailyReportService.create(request), MDC.get("traceId"));
    }

    @GetMapping
    public ApiResponse<PageResult<DailyReportResponse>> list(@Valid @ModelAttribute DailyReportQueryRequest request) {
        return ApiResponse.success(dailyReportService.list(request), MDC.get("traceId"));
    }

    @GetMapping("/my")
    public ApiResponse<List<DailyReportResponse>> listMine() {
        return ApiResponse.success(dailyReportService.listMine(), MDC.get("traceId"));
    }

    @GetMapping("/{id}")
    public ApiResponse<DailyReportResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(dailyReportService.getById(id), MDC.get("traceId"));
    }

    @PutMapping("/{id}")
    public ApiResponse<DailyReportResponse> update(@PathVariable Long id, @Valid @RequestBody DailyReportUpdateRequest request) {
        return ApiResponse.success(dailyReportService.update(id, request), MDC.get("traceId"));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        dailyReportService.delete(id);
        return ApiResponse.success(null, MDC.get("traceId"));
    }
}
