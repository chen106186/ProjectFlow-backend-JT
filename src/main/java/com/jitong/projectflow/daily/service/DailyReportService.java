package com.jitong.projectflow.daily.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jitong.projectflow.auth.security.BusinessAccessService;
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
import com.jitong.projectflow.daily.mapper.DailyReportTaskMapper;
import com.jitong.projectflow.file.entity.FileMetadata;
import com.jitong.projectflow.file.mapper.FileMetadataMapper;
import com.jitong.projectflow.system.audit.OperationLogService;
import com.jitong.projectflow.system.entity.SystemUser;
import com.jitong.projectflow.system.mapper.SystemUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DailyReportService {
    private final DailyReportMapper dailyReportMapper;
    private final OperationLogService operationLogService;
    private final BusinessAccessService businessAccessService;
    private final FileMetadataMapper fileMetadataMapper;
    private final DailyReportTaskMapper dailyReportTaskMapper;
    private final SystemUserMapper systemUserMapper;

    public DailyReportResponse create(DailyReportCreateRequest request) {
        DailyReportEntity entity = new DailyReportEntity();
        entity.setProjectId(request.getProjectId());
        entity.setReporterId(CurrentUserContext.userId());
        entity.setReportDate(request.getReportDate());
        entity.setContent(request.getContent());
        entity.setCreatedBy(CurrentUserContext.userIdOrNull());
        dailyReportMapper.insert(entity);
        if (request.getRelatedTaskIds() != null) {
            for (Long taskId : request.getRelatedTaskIds()) {
                dailyReportTaskMapper.insert(entity.getId(), taskId);
            }
        }
        operationLogService.record("daily-report", "DailyReport", entity.getId(), "CREATE",
                "提交日报：" + entity.getReportDate());
        return toResponse(entity, null);
    }

    public PageResult<DailyReportResponse> list(DailyReportQueryRequest request) {
        if (request.getReporterId() == null && !businessAccessService.isSystemAdmin() && !businessAccessService.canViewAll()) {
            request.setReporterId(CurrentUserContext.userId());
        }
        Page<DailyReportEntity> page = dailyReportMapper.selectPage(PageUtils.toPage(request), buildQuery(request));
        return PageUtils.toResult(page, page.getRecords().stream().map(e -> toResponse(e, null)).toList());
    }

    public List<DailyReportResponse> listMine() {
        if (businessAccessService.canViewAll()) {
            // 总经办：返回今日所有人的日报
            LocalDate today = LocalDate.now();
            List<DailyReportEntity> entities = dailyReportMapper.selectList(
                    new LambdaQueryWrapper<DailyReportEntity>()
                            .eq(DailyReportEntity::getReportDate, today)
                            .orderByDesc(DailyReportEntity::getCreatedAt));
            List<Long> reporterIds = entities.stream()
                    .map(DailyReportEntity::getReporterId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            Map<Long, String> nameMap = reporterIds.isEmpty() ? Map.of()
                    : systemUserMapper.selectByIds(reporterIds).stream()
                            .collect(Collectors.toMap(SystemUser::getId, u -> u.getRealName() != null ? u.getRealName() : u.getUsername()));
            return entities.stream().map(e -> toResponse(e, nameMap.get(e.getReporterId()))).toList();
        }
        DailyReportQueryRequest request = new DailyReportQueryRequest();
        request.setReporterId(CurrentUserContext.userId());
        return dailyReportMapper.selectList(buildQuery(request)).stream().map(e -> toResponse(e, null)).toList();
    }

    public DailyReportResponse getById(Long id) {
        return toResponse(requireReport(id), null);
    }

    public DailyReportResponse update(Long id, DailyReportUpdateRequest request) {
        DailyReportEntity entity = requireReport(id);
        businessAccessService.requireDailyReportManage(entity);
        if (request.getProjectId() != null) entity.setProjectId(request.getProjectId());
        if (request.getReportDate() != null) entity.setReportDate(request.getReportDate());
        if (request.getContent() != null) entity.setContent(request.getContent());
        entity.setUpdatedBy(CurrentUserContext.userIdOrNull());
        dailyReportMapper.updateById(entity);
        if (request.getRelatedTaskIds() != null) {
            dailyReportTaskMapper.deleteByReportId(id);
            for (Long taskId : request.getRelatedTaskIds()) {
                dailyReportTaskMapper.insert(id, taskId);
            }
        }
        operationLogService.record("daily-report", "DailyReport", id, "UPDATE",
                "编辑日报：" + entity.getReportDate());
        return toResponse(entity, null);
    }

    public void delete(Long id) {
        DailyReportEntity entity = requireReport(id);
        businessAccessService.requireDailyReportManage(entity);
        dailyReportMapper.deleteById(id);
        operationLogService.record("daily-report", "DailyReport", id, "DELETE",
                "删除日报：" + entity.getReportDate());
    }

    public int syncFilesToProject(Long reportId) {
        DailyReportEntity report = requireReport(reportId);
        Long projectId = report.getProjectId();

        List<FileMetadata> reportFiles = fileMetadataMapper.selectList(
                new LambdaQueryWrapper<FileMetadata>()
                        .eq(FileMetadata::getBusinessType, "DAILY_REPORT")
                        .eq(FileMetadata::getBusinessId, reportId));
        if (reportFiles.isEmpty()) {
            return 0;
        }

        List<FileMetadata> existingProjectFiles = fileMetadataMapper.selectList(
                new LambdaQueryWrapper<FileMetadata>()
                        .eq(FileMetadata::getBusinessType, "PROJECT")
                        .eq(FileMetadata::getBusinessId, projectId));
        Set<String> existingKeys = existingProjectFiles.stream()
                .map(FileMetadata::getStorageKey)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        int count = 0;
        for (FileMetadata src : reportFiles) {
            if (src.getStorageKey() != null && existingKeys.contains(src.getStorageKey())) {
                continue;
            }
            FileMetadata mirror = new FileMetadata();
            mirror.setBusinessType("PROJECT");
            mirror.setBusinessId(projectId);
            mirror.setOriginalName(src.getOriginalName());
            mirror.setContentType(src.getContentType());
            mirror.setFileSize(src.getFileSize());
            mirror.setVersionNo(src.getVersionNo());
            mirror.setStorageLocation(src.getStorageLocation());
            mirror.setFileCategory(src.getFileCategory());
            mirror.setStorageType(src.getStorageType());
            mirror.setStorageKey(src.getStorageKey());
            mirror.setUploaderId(src.getUploaderId());
            mirror.setUploadedAt(src.getUploadedAt());
            fileMetadataMapper.insert(mirror);
            count++;
        }
        return count;
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

    private DailyReportResponse toResponse(DailyReportEntity entity, String reporterName) {
        List<Long> relatedTaskIds = dailyReportTaskMapper.findTaskIdsByReportId(entity.getId());
        return DailyReportResponse.builder()
                .id(entity.getId())
                .projectId(entity.getProjectId())
                .reporterId(entity.getReporterId())
                .reporterName(reporterName)
                .reportDate(entity.getReportDate())
                .content(entity.getContent())
                .createdBy(entity.getCreatedBy())
                .createdAt(entity.getCreatedAt())
                .updatedBy(entity.getUpdatedBy())
                .updatedAt(entity.getUpdatedAt())
                .relatedTaskIds(relatedTaskIds)
                .build();
    }
}
