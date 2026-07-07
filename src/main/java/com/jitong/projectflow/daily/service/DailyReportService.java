package com.jitong.projectflow.daily.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jitong.projectflow.auth.security.CurrentUserContext;
import com.jitong.projectflow.common.api.PageResult;
import com.jitong.projectflow.common.api.PageUtils;
import com.jitong.projectflow.common.error.BusinessException;
import com.jitong.projectflow.common.error.ErrorCode;
import com.jitong.projectflow.daily.dto.DailyReportCreateRequest;
import com.jitong.projectflow.daily.dto.DailyReportQueryRequest;
import com.jitong.projectflow.daily.dto.DailyReportResponse;
import com.jitong.projectflow.daily.dto.DailyReportUpdateRequest;
import com.jitong.projectflow.daily.entity.DailyReportEntity;
import com.jitong.projectflow.daily.mapper.DailyReportMapper;
import com.jitong.projectflow.system.audit.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DailyReportService {
    private final DailyReportMapper dailyReportMapper;
    private final OperationLogService operationLogService;

    public DailyReportResponse create(DailyReportCreateRequest request) {
        DailyReportEntity entity = new DailyReportEntity();
        entity.setProjectId(request.getProjectId());
        entity.setReporterId(CurrentUserContext.userId());
        entity.setReportDate(request.getReportDate());
        entity.setContent(request.getContent());
        entity.setCreatedBy(CurrentUserContext.userIdOrNull());
        dailyReportMapper.insert(entity);
        operationLogService.record("daily-report", "DailyReport", entity.getId(), "CREATE", entity.getContent());
        return toResponse(entity);
    }

    public PageResult<DailyReportResponse> list(DailyReportQueryRequest request) {
        Page<DailyReportEntity> page = dailyReportMapper.selectPage(PageUtils.toPage(request), buildQuery(request));
        return PageUtils.toResult(page, page.getRecords().stream().map(this::toResponse).toList());
    }

    public List<DailyReportResponse> listMine() {
        DailyReportQueryRequest request = new DailyReportQueryRequest();
        request.setReporterId(CurrentUserContext.userId());
        return dailyReportMapper.selectList(buildQuery(request)).stream().map(this::toResponse).toList();
    }

    public DailyReportResponse getById(Long id) {
        return toResponse(requireReport(id));
    }

    public DailyReportResponse update(Long id, DailyReportUpdateRequest request) {
        DailyReportEntity entity = requireReport(id);
        if (request.getProjectId() != null) entity.setProjectId(request.getProjectId());
        if (request.getReportDate() != null) entity.setReportDate(request.getReportDate());
        if (request.getContent() != null) entity.setContent(request.getContent());
        entity.setUpdatedBy(CurrentUserContext.userIdOrNull());
        dailyReportMapper.updateById(entity);
        operationLogService.record("daily-report", "DailyReport", id, "UPDATE", entity.getContent());
        return toResponse(entity);
    }

    public void delete(Long id) {
        DailyReportEntity entity = requireReport(id);
        dailyReportMapper.deleteById(id);
        operationLogService.record("daily-report", "DailyReport", id, "DELETE", entity.getContent());
    }

    private LambdaQueryWrapper<DailyReportEntity> buildQuery(DailyReportQueryRequest request) {
        LambdaQueryWrapper<DailyReportEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(request.getProjectId() != null, DailyReportEntity::getProjectId, request.getProjectId());
        wrapper.eq(request.getReporterId() != null, DailyReportEntity::getReporterId, request.getReporterId());
        wrapper.ge(request.getDateFrom() != null, DailyReportEntity::getReportDate, request.getDateFrom());
        wrapper.le(request.getDateTo() != null, DailyReportEntity::getReportDate, request.getDateTo());
        wrapper.like(StringUtils.hasText(request.getKeyword()), DailyReportEntity::getContent, request.getKeyword());
        wrapper.orderByDesc(DailyReportEntity::getReportDate);
        wrapper.orderByDesc(DailyReportEntity::getCreatedAt);
        return wrapper;
    }

    private DailyReportEntity requireReport(Long id) {
        DailyReportEntity entity = dailyReportMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "日报不存在");
        }
        return entity;
    }

    private DailyReportResponse toResponse(DailyReportEntity entity) {
        return DailyReportResponse.builder()
                .id(entity.getId())
                .projectId(entity.getProjectId())
                .reporterId(entity.getReporterId())
                .reportDate(entity.getReportDate())
                .content(entity.getContent())
                .createdBy(entity.getCreatedBy())
                .createdAt(entity.getCreatedAt())
                .updatedBy(entity.getUpdatedBy())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
