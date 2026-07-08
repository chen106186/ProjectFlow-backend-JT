package com.jitong.projectflow.daily.controller;

import com.jitong.projectflow.common.api.ApiResponse;
import com.jitong.projectflow.common.api.PageResult;
import com.jitong.projectflow.daily.dto.DailyReportCreateRequest;
import com.jitong.projectflow.daily.dto.DailyReportQueryRequest;
import com.jitong.projectflow.daily.dto.DailyReportResponse;
import com.jitong.projectflow.daily.dto.DailyReportUpdateRequest;
import com.jitong.projectflow.daily.service.DailyReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "日报管理", description = "日报填写、查询、编辑和删除接口")
public class DailyReportController {
    private final DailyReportService dailyReportService;

    @Operation(summary = "填写日报",
            description = "当前登录用户填写指定项目和日期的日报内容。")
    @PostMapping
    @PreAuthorize("hasAuthority('daily-report:create')")
    public ApiResponse<DailyReportResponse> create(@Valid @RequestBody DailyReportCreateRequest request) {
        return ApiResponse.success(dailyReportService.create(request), MDC.get("traceId"));
    }

    @Operation(summary = "分页查询日报列表",
            description = "按项目、填写人、日期范围和关键字分页查询日报。")
    @GetMapping
    public ApiResponse<PageResult<DailyReportResponse>> list(@Valid @ModelAttribute DailyReportQueryRequest request) {
        return ApiResponse.success(dailyReportService.list(request), MDC.get("traceId"));
    }

    @Operation(summary = "查询我的日报",
            description = "查询当前登录用户填写的全部日报记录。")
    @GetMapping("/my")
    public ApiResponse<List<DailyReportResponse>> listMine() {
        return ApiResponse.success(dailyReportService.listMine(), MDC.get("traceId"));
    }

    @Operation(summary = "查询日报详情",
            description = "根据日报 ID 查询项目、填写人、日报日期和日报内容。")
    @GetMapping("/{id}")
    public ApiResponse<DailyReportResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(dailyReportService.getById(id), MDC.get("traceId"));
    }

    @Operation(summary = "编辑日报",
            description = "修改日报项目、日期或内容，仅日报本人、创建人或系统管理员可操作。")
    @PutMapping("/{id}")
    public ApiResponse<DailyReportResponse> update(@PathVariable Long id, @Valid @RequestBody DailyReportUpdateRequest request) {
        return ApiResponse.success(dailyReportService.update(id, request), MDC.get("traceId"));
    }

    @Operation(summary = "删除日报",
            description = "逻辑删除指定日报，仅日报本人、创建人或系统管理员可操作。")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        dailyReportService.delete(id);
        return ApiResponse.success(null, MDC.get("traceId"));
    }

    @Operation(summary = "同步日报文件到项目",
            description = "将指定日报下的附件文件同步关联到该日报所属项目，已存在的文件不重复同步。")
    @PostMapping("/{id}/sync-files")
    public ApiResponse<Integer> syncFiles(@PathVariable Long id) {
        return ApiResponse.success(dailyReportService.syncFilesToProject(id), MDC.get("traceId"));
    }
}
