package com.jitong.projectflow.requirement.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jitong.projectflow.auth.security.BusinessAccessService;
import com.jitong.projectflow.auth.security.CurrentUserContext;
import com.jitong.projectflow.common.error.BusinessException;
import com.jitong.projectflow.common.error.ErrorCode;
import com.jitong.projectflow.common.service.BusinessNumberService;
import com.jitong.projectflow.notice.domain.NoticeType;
import com.jitong.projectflow.notice.service.NoticeService;
import com.jitong.projectflow.project.entity.ProjectEntity;
import com.jitong.projectflow.project.mapper.ProjectMapper;
import com.jitong.projectflow.requirement.dto.RequirementCreateRequest;
import com.jitong.projectflow.requirement.dto.RequirementStatusUpdateRequest;
import com.jitong.projectflow.requirement.dto.RequirementUpdateRequest;
import com.jitong.projectflow.requirement.dto.RequirementQueryRequest;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import com.jitong.projectflow.requirement.entity.RequirementEntity;
import com.jitong.projectflow.requirement.mapper.RequirementMapper;
import com.jitong.projectflow.system.audit.OperationLogService;
import com.jitong.projectflow.system.mapper.OperationLogMapper;
import com.jitong.projectflow.system.mapper.SystemUserMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequirementServiceTest {
    @Mock RequirementMapper requirementMapper;
    @Mock SystemUserMapper systemUserMapper;
    @Mock ProjectMapper projectMapper;
    @Mock OperationLogService operationLogService;
    @Mock OperationLogMapper operationLogMapper;
    @Mock NoticeService noticeService;
    @Mock BusinessAccessService businessAccessService;
    @Mock BusinessNumberService businessNumberService;
    @InjectMocks RequirementService service;

    @AfterEach void clearContext() { CurrentUserContext.clear(); }

    @Test
    void reviewerIdBindsFromJsonAndIsPersistedAndNotified() throws Exception {
        CurrentUserContext.set(1L);
        ProjectEntity project = new ProjectEntity();
        project.setId(10L);
        when(projectMapper.selectById(10L)).thenReturn(project);
        when(businessNumberService.nextRequirementNo()).thenReturn(5L);
        var request = new ObjectMapper().readValue("{\"title\":\"New feature\",\"projectId\":10,\"reviewerId\":2}", RequirementCreateRequest.class);
        service.create(request);
        ArgumentCaptor<RequirementEntity> entity = ArgumentCaptor.forClass(RequirementEntity.class);
        verify(requirementMapper).insert(entity.capture());
        assertThat(entity.getValue().getReviewerId()).isEqualTo(2L);
        assertThat(entity.getValue().getRequirementNo()).isEqualTo(5L);
        verify(noticeService).create(eq(2L), eq(NoticeType.REQUIREMENT_STATUS_CHANGED), eq("您有一条需求待评审"), eq("New feature"), eq("Requirement"), isNull());
        assertThat(new ObjectMapper().readValue("{\"reviewerId\":3}", RequirementUpdateRequest.class).reviewerId()).isEqualTo(3L);
    }

    @Test
    void statusEndpointUsesReviewerPermissionAndNotifiesOwner() {
        CurrentUserContext.set(2L);
        RequirementEntity entity = requirement("PENDING_REVIEW");
        when(requirementMapper.selectById(9L)).thenReturn(entity);
        var request = new RequirementStatusUpdateRequest();
        request.setStatus("ACCEPTED");
        service.updateStatus(9L, request);
        verify(businessAccessService).requireRequirementReview(entity);
        verify(businessAccessService, never()).requireRequirementOwner(entity);
        assertThat(entity.getStatus()).isEqualTo("ACCEPTED");
        verify(noticeService).create(1L, NoticeType.REQUIREMENT_STATUS_CHANGED, "需求状态变更", "需求A：待评审 -> 已采纳", "Requirement", 9L);
    }

    @Test
    void ordinaryUpdateCannotBypassReviewPermission() {
        CurrentUserContext.set(1L);
        RequirementEntity entity = requirement("PENDING_REVIEW");
        when(requirementMapper.selectById(9L)).thenReturn(entity);
        doThrow(new BusinessException(ErrorCode.FORBIDDEN, "需要评审权限")).when(businessAccessService).requireRequirementReview(entity);
        var request = new RequirementUpdateRequest();
        request.setStatus("ACCEPTED");
        assertThatThrownBy(() -> service.update(9L, request)).isInstanceOf(BusinessException.class);
        verify(requirementMapper, never()).updateById(any(RequirementEntity.class));
        assertThat(entity.getStatus()).isEqualTo("PENDING_REVIEW");
    }

    @Test
    void ordinaryUpdateCannotReverseFinalDecision() {
        CurrentUserContext.set(2L);
        RequirementEntity entity = requirement("ACCEPTED");
        when(requirementMapper.selectById(9L)).thenReturn(entity);
        var request = new RequirementUpdateRequest();
        request.setStatus("PENDING_REVIEW");
        assertThatThrownBy(() -> service.update(9L, request)).isInstanceOf(BusinessException.class)
                .hasMessage("需求状态流转不合法");
        verify(requirementMapper, never()).updateById(any(RequirementEntity.class));
    }

    @Test
    void mineOnlyAddsGroupedOwnerOrReviewerFilterEvenForViewAllUsers() {
        CurrentUserContext.set(1L);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), RequirementEntity.class);
        when(businessAccessService.canViewAll()).thenReturn(true);
        when(requirementMapper.selectPage(any(), any())).thenAnswer(invocation -> {
            LambdaQueryWrapper<RequirementEntity> query = invocation.getArgument(1);
            String sql = query.getSqlSegment().replaceAll("#\\{[^}]+}", "?");
            assertThat(sql).contains("status = ? AND (created_by = ? OR reviewer_id = ?)");
            assertThat(query.getParamNameValuePairs().values()).contains("PENDING_REVIEW", 1L);
            assertThat(query.getParamNameValuePairs().values().stream().filter(value -> Long.valueOf(1L).equals(value)).count()).isEqualTo(2);
            return new Page<RequirementEntity>(1, 20, 0);
        });
        RequirementQueryRequest request = new RequirementQueryRequest();
        request.setMineOnly(true);
        request.setStatus("PENDING_REVIEW");
        assertThat(service.list(request).records()).isEmpty();
        verify(businessAccessService, never()).readableProjectIdsSql();
    }

    private RequirementEntity requirement(String status) {
        var entity = new RequirementEntity();
        entity.setId(9L);
        entity.setTitle("需求A");
        entity.setStatus(status);
        entity.setCreatedBy(1L);
        entity.setReviewerId(2L);
        return entity;
    }
}
