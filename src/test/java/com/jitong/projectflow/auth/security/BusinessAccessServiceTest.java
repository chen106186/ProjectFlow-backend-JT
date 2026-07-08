package com.jitong.projectflow.auth.security;

import com.jitong.projectflow.bug.entity.BugEntity;
import com.jitong.projectflow.common.error.BusinessException;
import com.jitong.projectflow.file.entity.FileMetadata;
import com.jitong.projectflow.project.entity.ProjectEntity;
import com.jitong.projectflow.project.mapper.ProjectMapper;
import com.jitong.projectflow.requirement.entity.RequirementEntity;
import com.jitong.projectflow.task.entity.TaskEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BusinessAccessServiceTest {
    @Mock
    ProjectMapper projectMapper;

    @AfterEach
    void clearContext() {
        CurrentUserContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void systemAdminCanManageAnyProject() {
        authenticate(9001L, "system:user:update");
        ProjectEntity project = new ProjectEntity();
        project.setManagerId(1001L);
        project.setCreatedBy(1002L);

        assertThatCode(() -> new BusinessAccessService(projectMapper).requireProjectManage(project))
                .doesNotThrowAnyException();
    }

    @Test
    void projectManagerCanManageProject() {
        authenticate(1001L);
        ProjectEntity project = new ProjectEntity();
        project.setManagerId(1001L);

        assertThatCode(() -> new BusinessAccessService(projectMapper).requireProjectManage(project))
                .doesNotThrowAnyException();
    }

    @Test
    void unrelatedUserCannotManageProject() {
        authenticate(3001L);
        ProjectEntity project = new ProjectEntity();
        project.setManagerId(1001L);
        project.setCreatedBy(1002L);

        assertThatThrownBy(() -> new BusinessAccessService(projectMapper).requireProjectManage(project))
                .isInstanceOf(BusinessException.class)
                .hasMessage("无权操作该数据");
    }

    @Test
    void taskAssigneeCanUpdateActualTime() {
        authenticate(2001L);
        TaskEntity task = new TaskEntity();
        task.setAssigneeId(2001L);

        assertThatCode(() -> new BusinessAccessService(projectMapper).requireTaskActualTimeManage(task))
                .doesNotThrowAnyException();
    }

    @Test
    void projectManagerCanManageTask() {
        authenticate(1001L);
        TaskEntity task = new TaskEntity();
        task.setProjectId(10L);
        ProjectEntity project = new ProjectEntity();
        project.setManagerId(1001L);
        when(projectMapper.selectById(10L)).thenReturn(project);

        assertThatCode(() -> new BusinessAccessService(projectMapper).requireTaskManage(task))
                .doesNotThrowAnyException();
    }

    @Test
    void bugAssigneeCanEditBugButCannotCloseIt() {
        authenticate(2001L);
        BugEntity bug = new BugEntity();
        bug.setCreatorId(1001L);
        bug.setAssigneeId(2001L);

        BusinessAccessService service = new BusinessAccessService(projectMapper);
        assertThatCode(() -> service.requireBugEdit(bug)).doesNotThrowAnyException();
        assertThatThrownBy(() -> service.requireBugClose(bug))
                .isInstanceOf(BusinessException.class)
                .hasMessage("无权操作该数据");
    }

    @Test
    void fileUploaderCanDeleteFile() {
        authenticate(1001L);
        FileMetadata metadata = new FileMetadata();
        metadata.setUploaderId(1001L);

        assertThatCode(() -> new BusinessAccessService(projectMapper).requireFileDelete(metadata))
                .doesNotThrowAnyException();
    }

    @Test
    void projectManagerCanManageRequirement() {
        authenticate(1001L);
        RequirementEntity requirement = new RequirementEntity();
        requirement.setProjectId(10L);
        ProjectEntity project = new ProjectEntity();
        project.setManagerId(1001L);
        when(projectMapper.selectById(10L)).thenReturn(project);

        assertThatCode(() -> new BusinessAccessService(projectMapper).requireRequirementManage(requirement))
                .doesNotThrowAnyException();
    }

    private void authenticate(Long userId, String... authorities) {
        CurrentUserContext.set(userId);
        List<SimpleGrantedAuthority> grantedAuthorities = java.util.Arrays.stream(authorities)
                .map(SimpleGrantedAuthority::new)
                .toList();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null, grantedAuthorities));
    }
}
