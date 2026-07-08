package com.jitong.projectflow.bug.service;

import com.jitong.projectflow.auth.security.CurrentUserContext;
import com.jitong.projectflow.auth.security.BusinessAccessService;
import com.jitong.projectflow.bug.domain.BugStatus;
import com.jitong.projectflow.bug.dto.BugAssignRequest;
import com.jitong.projectflow.bug.dto.BugCommentCreateRequest;
import com.jitong.projectflow.bug.entity.BugCommentEntity;
import com.jitong.projectflow.bug.entity.BugEntity;
import com.jitong.projectflow.bug.mapper.BugCommentMapper;
import com.jitong.projectflow.bug.mapper.BugMapper;
import com.jitong.projectflow.notice.domain.NoticeType;
import com.jitong.projectflow.notice.service.NoticeService;
import com.jitong.projectflow.system.audit.OperationLogService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
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

        new BugService(bugMapper, bugCommentMapper, operationLogService, noticeService, businessAccessService).assign(10L, request);

        assertThat(bug.getAssigneeId()).isEqualTo(2002L);
        verify(businessAccessService).requireBugEdit(bug);
        verify(bugMapper).updateById(bug);
        verify(noticeService).create(2002L, NoticeType.BUG_ASSIGNED, "缺陷转派通知", "Login fails", "Bug", 10L);
        verify(noticeService).create(1001L, NoticeType.BUG_ASSIGNED, "缺陷转派抄送",
                "Login fails 已转派给用户 2002", "Bug", 10L);
        verify(operationLogService).record("bug", "Bug", 10L, "ASSIGN", "handoff");
    }

    @Test
    void closeSetsStatusClosed() {
        CurrentUserContext.set(1001L);
        BugEntity bug = bug(10L);
        when(bugMapper.selectById(10L)).thenReturn(bug);

        new BugService(bugMapper, bugCommentMapper, operationLogService, noticeService, businessAccessService).close(10L);

        assertThat(bug.getStatus()).isEqualTo(BugStatus.CLOSED.name());
        assertThat(bug.getClosedAt()).isNotNull();
        verify(businessAccessService).requireBugClose(bug);
        verify(operationLogService).record("bug", "Bug", 10L, "CLOSE", "Login fails");
    }

    @Test
    void addCommentPersistsCommentAndCreatesNotice() {
        CurrentUserContext.set(1001L);
        BugEntity bug = bug(10L);
        when(bugMapper.selectById(10L)).thenReturn(bug);
        BugCommentCreateRequest request = new BugCommentCreateRequest();
        request.setContent("please verify");

        new BugService(bugMapper, bugCommentMapper, operationLogService, noticeService, businessAccessService).addComment(10L, request);

        ArgumentCaptor<BugCommentEntity> captor = ArgumentCaptor.forClass(BugCommentEntity.class);
        verify(businessAccessService).requireBugEdit(bug);
        verify(bugCommentMapper).insert(captor.capture());
        assertThat(captor.getValue().getBugId()).isEqualTo(10L);
        assertThat(captor.getValue().getUserId()).isEqualTo(1001L);
        assertThat(captor.getValue().getContent()).isEqualTo("please verify");
        verify(noticeService).create(2002L, NoticeType.BUG_COMMENT, "缺陷评论通知", "please verify", "Bug", 10L);
    }

    private BugEntity bug(Long id) {
        BugEntity bug = new BugEntity();
        bug.setId(id);
        bug.setTitle("Login fails");
        bug.setCreatorId(1001L);
        bug.setAssigneeId(2002L);
        bug.setStatus(BugStatus.PENDING_FIX.name());
        return bug;
    }
}
