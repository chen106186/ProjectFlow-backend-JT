package com.jitong.projectflow.bug.service;

import com.jitong.projectflow.auth.security.CurrentUserContext;
import com.jitong.projectflow.auth.security.BusinessAccessService;
import com.jitong.projectflow.bug.domain.BugStatus;
import com.jitong.projectflow.bug.dto.BugAssignRequest;
import com.jitong.projectflow.bug.dto.BugCommentCreateRequest;
import com.jitong.projectflow.bug.dto.BugUpdateRequest;
import com.jitong.projectflow.bug.dto.BugQueryRequest;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import com.jitong.projectflow.common.error.BusinessException;
import com.jitong.projectflow.common.error.ErrorCode;
import com.jitong.projectflow.common.service.BusinessNumberService;
import com.jitong.projectflow.bug.entity.BugCommentEntity;
import com.jitong.projectflow.bug.entity.BugEntity;
import com.jitong.projectflow.bug.mapper.BugCommentMapper;
import com.jitong.projectflow.bug.mapper.BugMapper;
import com.jitong.projectflow.bug.mapper.BugTaskMapper;
import com.jitong.projectflow.task.mapper.TaskMapper;
import com.jitong.projectflow.project.mapper.ProjectMapper;
import com.jitong.projectflow.system.mapper.SystemUserMapper;
import com.jitong.projectflow.system.mapper.OperationLogMapper;
import com.jitong.projectflow.notice.domain.NoticeType;
import com.jitong.projectflow.notice.service.NoticeService;
import com.jitong.projectflow.system.audit.OperationLogService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BugServiceTest {
    @Mock
    BugMapper bugMapper;
    @Mock
    BugCommentMapper bugCommentMapper;
    @Mock
    OperationLogService operationLogService;
    @Mock
    NoticeService noticeService;
    @Mock
    BusinessAccessService businessAccessService;
    @Mock SystemUserMapper userMapper;
    @Mock ProjectMapper projectMapper;
    @Mock OperationLogMapper operationLogMapper;
    @Mock BugTaskMapper bugTaskMapper;
    @Mock TaskMapper taskMapper;
    @Mock BusinessNumberService businessNumberService;
    @InjectMocks BugService bugService;

    @AfterEach
    void clearCurrentUser() {
        CurrentUserContext.clear();
    }

    @Test
    void assignChangesAssigneeAndCreatesNotice() {
        CurrentUserContext.set(1001L);
        BugEntity bug = bug(10L);
        when(bugMapper.selectById(10L)).thenReturn(bug);
        BugAssignRequest request = new BugAssignRequest();
        request.setAssigneeId(2002L);
        request.setReason("handoff");

        bugService.assign(10L, request);

        assertThat(bug.getAssigneeId()).isEqualTo(2002L);
        verify(businessAccessService).requireBugEdit(bug);
        verify(bugMapper).updateById(bug);
        verify(noticeService).create(2002L, NoticeType.BUG_ASSIGNED, "未知用户 将缺陷「Login fails」改派给您", "所属项目：未知项目　｜　提交人：未知用户", "Bug", 10L);
        verify(noticeService).create(1001L, NoticeType.BUG_ASSIGNED, "缺陷「Login fails」已转派给 未知用户",
                "所属项目：未知项目　｜　转派人：未知用户", "Bug", 10L);
        verify(operationLogService).record("bug", "Bug", 10L, "ASSIGN", "转派Bug：Login fails，原因：handoff");
    }

    @Test
    void closeSetsStatusClosed() {
        CurrentUserContext.set(1001L);
        BugEntity bug = bug(10L);
        when(bugMapper.selectById(10L)).thenReturn(bug);

        bugService.close(10L);

        assertThat(bug.getStatus()).isEqualTo(BugStatus.CLOSED.name());
        assertThat(bug.getClosedAt()).isNotNull();
        verify(businessAccessService).requireBugClose(bug);
        verify(operationLogService).record("bug", "Bug", 10L, "CLOSE", "Bug状态由待修复变为已关闭：Login fails");
    }

    @Test
    void addCommentPersistsCommentAndCreatesNotice() {
        CurrentUserContext.set(1001L);
        BugEntity bug = bug(10L);
        when(bugMapper.selectById(10L)).thenReturn(bug);
        BugCommentCreateRequest request = new BugCommentCreateRequest();
        request.setContent("please verify");

        bugService.addComment(10L, request);

        ArgumentCaptor<BugCommentEntity> captor = ArgumentCaptor.forClass(BugCommentEntity.class);
        verify(businessAccessService).requireBugEdit(bug);
        verify(bugCommentMapper).insert(captor.capture());
        assertThat(captor.getValue().getBugId()).isEqualTo(10L);
        assertThat(captor.getValue().getUserId()).isEqualTo(1001L);
        assertThat(captor.getValue().getContent()).isEqualTo("please verify");
        verify(noticeService).create(2002L, NoticeType.BUG_COMMENT, "未知用户 在缺陷「Login fails」下发了评论", "所属缺陷：「Login fails」　｜　提交人：未知用户", "Bug", 10L);
    }

    @Test
    void ordinaryUpdateCannotBypassClosePermission() {
        CurrentUserContext.set(2002L);
        BugEntity bug = bug(10L);
        when(bugMapper.selectById(10L)).thenReturn(bug);
        doThrow(new BusinessException(ErrorCode.FORBIDDEN, "无关闭权限")).when(businessAccessService).requireBugClose(bug);
        BugUpdateRequest request = new BugUpdateRequest();
        request.setStatus("CLOSED");
        assertThatThrownBy(() -> bugService.update(10L, request)).isInstanceOf(BusinessException.class);
        verify(bugMapper, never()).updateById(any(BugEntity.class));
        assertThat(bug.getStatus()).isEqualTo("PENDING_FIX");
    }

    @Test
    void ordinaryUpdateClosingAndReopeningUsesSameStateSideEffects() {
        CurrentUserContext.set(1001L);
        BugEntity bug = bug(10L);
        when(bugMapper.selectById(10L)).thenReturn(bug);
        BugUpdateRequest close = new BugUpdateRequest();
        close.setStatus("CLOSED");
        bugService.update(10L, close);
        assertThat(bug.getClosedAt()).isNotNull();
        BugUpdateRequest reopen = new BugUpdateRequest();
        reopen.setStatus("PENDING_FIX");
        bugService.update(10L, reopen);
        assertThat(bug.getStatus()).isEqualTo("PENDING_FIX");
        assertThat(bug.getClosedAt()).isNull();
        verify(businessAccessService, org.mockito.Mockito.times(2)).requireBugClose(bug);
    }

    @Test
    void ordinaryUpdateToVerificationNotifiesCreator() {
        CurrentUserContext.set(2002L);
        BugEntity bug = bug(10L);
        when(bugMapper.selectById(10L)).thenReturn(bug);
        BugUpdateRequest request = new BugUpdateRequest();
        request.setStatus("PENDING_VERIFY");
        bugService.update(10L, request);
        verify(noticeService).create(1001L, NoticeType.BUG_ASSIGNED,
                "缺陷「Login fails」已提交处理，待您验证", "所属项目：未知项目　｜　处理人：未知用户", "Bug", 10L);
    }

    @Test
    void mineOnlyAddsGroupedOwnerOrAssigneeFilterEvenForViewAllUsers() {
        CurrentUserContext.set(1001L);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), BugEntity.class);
        when(businessAccessService.canViewAll()).thenReturn(true);
        when(bugMapper.selectPage(any(), any())).thenAnswer(invocation -> {
            LambdaQueryWrapper<BugEntity> query = invocation.getArgument(1);
            String sql = query.getSqlSegment().replaceAll("#\\{[^}]+}", "?");
            assertThat(sql).contains("status = ? AND (creator_id = ? OR assignee_id = ?)");
            assertThat(query.getParamNameValuePairs().values()).contains("PENDING_FIX", 1001L);
            assertThat(query.getParamNameValuePairs().values().stream().filter(value -> Long.valueOf(1001L).equals(value)).count()).isEqualTo(2);
            return new Page<BugEntity>(1, 20, 0);
        });
        BugQueryRequest request = new BugQueryRequest();
        request.setMineOnly(true);
        request.setStatus("PENDING_FIX");
        assertThat(bugService.list(request).records()).isEmpty();
        verify(businessAccessService, never()).readableProjectIdsSql();
    }

    @Test
    void listFiltersCreatedTimeByInclusiveCalendarDays() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), BugEntity.class);
        when(businessAccessService.canViewAll()).thenReturn(true);
        when(bugMapper.selectPage(any(), any())).thenAnswer(invocation -> {
            LambdaQueryWrapper<BugEntity> query = invocation.getArgument(1);
            String sql = query.getSqlSegment().replaceAll("#\\{[^}]+}", "?");
            assertThat(sql).contains("created_at >= ?", "created_at < ?");
            assertThat(query.getParamNameValuePairs().values())
                    .contains(LocalDateTime.of(2026, 10, 1, 0, 0), LocalDateTime.of(2026, 11, 1, 0, 0));
            return new Page<BugEntity>(1, 20, 0);
        });
        BugQueryRequest request = new BugQueryRequest();
        request.setCreatedFrom(LocalDate.of(2026, 10, 1));
        request.setCreatedTo(LocalDate.of(2026, 10, 31));

        assertThat(bugService.list(request).records()).isEmpty();
    }

    @Test
    void listRejectsReversedCreatedTimeRange() {
        BugQueryRequest request = new BugQueryRequest();
        request.setCreatedFrom(LocalDate.of(2026, 10, 31));
        request.setCreatedTo(LocalDate.of(2026, 10, 1));

        assertThatThrownBy(() -> bugService.list(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("创建时间的开始日期不能晚于结束日期");
        verify(bugMapper, never()).selectPage(any(), any());
    }

    private BugEntity bug(Long id) {
        BugEntity bug = new BugEntity();
        bug.setId(id);
        bug.setProjectId(20L);
        bug.setTitle("Login fails");
        bug.setCreatorId(1001L);
        bug.setAssigneeId(2002L);
        bug.setStatus(BugStatus.PENDING_FIX.name());
        return bug;
    }
}
