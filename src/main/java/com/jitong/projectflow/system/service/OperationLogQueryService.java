package com.jitong.projectflow.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jitong.projectflow.common.api.PageResult;
import com.jitong.projectflow.common.api.PageUtils;
import com.jitong.projectflow.system.dto.OperationLogQueryRequest;
import com.jitong.projectflow.system.dto.OperationLogResponse;
import com.jitong.projectflow.system.entity.OperationLog;
import com.jitong.projectflow.system.mapper.OperationLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OperationLogQueryService {
    private final OperationLogMapper operationLogMapper;

    public PageResult<OperationLogResponse> list(OperationLogQueryRequest request) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(request.getModule()), OperationLog::getModule, request.getModule());
        wrapper.eq(StringUtils.hasText(request.getBusinessType()), OperationLog::getBusinessType, request.getBusinessType());
        wrapper.eq(request.getBusinessId() != null, OperationLog::getBusinessId, request.getBusinessId());
        wrapper.eq(StringUtils.hasText(request.getOperationType()), OperationLog::getOperationType, request.getOperationType());
        wrapper.eq(request.getOperatorId() != null, OperationLog::getOperatorId, request.getOperatorId());
        wrapper.like(StringUtils.hasText(request.getOperatorName()), OperationLog::getOperatorName, request.getOperatorName());
        wrapper.like(StringUtils.hasText(request.getKeyword()), OperationLog::getContent, request.getKeyword());
        wrapper.ge(request.getStartDate() != null, OperationLog::getCreatedAt,
                request.getStartDate() != null ? request.getStartDate().atStartOfDay() : null);
        wrapper.le(request.getEndDate() != null, OperationLog::getCreatedAt,
                request.getEndDate() != null ? request.getEndDate().atTime(23, 59, 59) : null);
        wrapper.orderByDesc(OperationLog::getCreatedAt);
        Page<OperationLog> page = operationLogMapper.selectPage(PageUtils.toPage(request), wrapper);
        return PageUtils.toResult(page, page.getRecords().stream().map(this::toResponse).toList());
    }

    private OperationLogResponse toResponse(OperationLog log) {
        return OperationLogResponse.builder()
                .id(log.getId())
                .module(log.getModule())
                .businessType(log.getBusinessType())
                .businessId(log.getBusinessId())
                .operationType(log.getOperationType())
                .operatorId(log.getOperatorId())
                .operatorName(log.getOperatorName())
                .content(log.getContent())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
