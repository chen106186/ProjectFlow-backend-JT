package com.jitong.projectflow.dict.controller;

import com.jitong.projectflow.common.api.ApiResponse;
import com.jitong.projectflow.dict.dto.DictGroupResponse;
import com.jitong.projectflow.dict.service.DictService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dicts")
@RequiredArgsConstructor
@Tag(name = "基础字典", description = "前端下拉框、筛选项和状态展示使用的固定字典接口")
public class DictController {
    private final DictService dictService;

    @Operation(summary = "查询全部基础字典",
            description = "一次性返回项目、任务、Bug、需求、汇报、通知、菜单和操作类型等基础字典。")
    @GetMapping
    public ApiResponse<List<DictGroupResponse>> listAll() {
        return ApiResponse.success(dictService.listAll(), MDC.get("traceId"));
    }

    @Operation(summary = "按类型查询基础字典",
            description = "根据字典类型编码查询单组字典项，例如 taskStatus、bugStatus、projectType。")
    @GetMapping("/{type}")
    public ApiResponse<DictGroupResponse> getByType(@PathVariable String type) {
        return ApiResponse.success(dictService.getByType(type), MDC.get("traceId"));
    }
}
