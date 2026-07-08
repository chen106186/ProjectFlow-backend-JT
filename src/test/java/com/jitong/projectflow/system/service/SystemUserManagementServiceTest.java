package com.jitong.projectflow.system.service;

import com.jitong.projectflow.auth.security.CurrentUserContext;
import com.jitong.projectflow.system.audit.OperationLogService;
import com.jitong.projectflow.system.dto.UserCreateRequest;
import com.jitong.projectflow.system.dto.UserEnabledUpdateRequest;
import com.jitong.projectflow.system.dto.UserRoleAssignRequest;
import com.jitong.projectflow.system.entity.SystemUser;
import com.jitong.projectflow.system.mapper.SystemUserMapper;
import com.jitong.projectflow.system.mapper.UserRoleMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemUserManagementServiceTest {
    @Mock
    SystemUserMapper systemUserMapper;
    @Mock
    UserRoleMapper userRoleMapper;
    @Mock
    PasswordEncoder passwordEncoder;
    @Mock
    OperationLogService operationLogService;

    @AfterEach
    void clearCurrentUser() {
        CurrentUserContext.clear();
    }

    @Test
    void createHashesPasswordAndWritesLog() {
        CurrentUserContext.set(99L);
        when(systemUserMapper.selectCount(any())).thenReturn(0L);
        when(passwordEncoder.encode("secret")).thenReturn("hashed");
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("lisi");
        request.setRealName("Li Si");
        request.setPassword("secret");
        request.setEnabled(true);

        new SystemUserManagementService(systemUserMapper, userRoleMapper, passwordEncoder, operationLogService)
                .create(request);

        ArgumentCaptor<SystemUser> captor = ArgumentCaptor.forClass(SystemUser.class);
        verify(systemUserMapper).insert(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("hashed");
        assertThat(captor.getValue().getCreatedBy()).isEqualTo(99L);
        verify(operationLogService).record("system", "User", captor.getValue().getId(), "CREATE", "Create user lisi");
    }

    @Test
    void createAssignsInitialRoles() {
        when(systemUserMapper.selectCount(any())).thenReturn(0L);
        when(passwordEncoder.encode("secret")).thenReturn("hashed");
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("wangwu");
        request.setRealName("Wang Wu");
        request.setPassword("secret");
        request.setRoleIds(List.of(10L, 20L));

        var response = new SystemUserManagementService(systemUserMapper, userRoleMapper, passwordEncoder, operationLogService)
                .create(request);

        ArgumentCaptor<SystemUser> captor = ArgumentCaptor.forClass(SystemUser.class);
        verify(systemUserMapper).insert(captor.capture());
        verify(userRoleMapper).insertRelation(captor.getValue().getId(), 10L);
        verify(userRoleMapper).insertRelation(captor.getValue().getId(), 20L);
        assertThat(response.getRoleIds()).containsExactly(10L, 20L);
    }

    @Test
    void updateEnabledChangesFlagAndWritesLog() {
        SystemUser user = new SystemUser();
        user.setId(1L);
        user.setUsername("lisi");
        user.setEnabled(true);
        when(systemUserMapper.selectById(1L)).thenReturn(user);
        UserEnabledUpdateRequest request = new UserEnabledUpdateRequest();
        request.setEnabled(false);

        new SystemUserManagementService(systemUserMapper, userRoleMapper, passwordEncoder, operationLogService)
                .updateEnabled(1L, request);

        assertThat(user.getEnabled()).isFalse();
        verify(systemUserMapper).updateById(user);
        verify(operationLogService).record("system", "User", 1L, "ENABLE_CHANGED", "Disable user lisi");
    }

    @Test
    void assignRolesReplacesExistingRows() {
        when(systemUserMapper.selectById(1L)).thenReturn(new SystemUser());
        UserRoleAssignRequest request = new UserRoleAssignRequest();
        request.setRoleIds(List.of(10L, 20L));

        new SystemUserManagementService(systemUserMapper, userRoleMapper, passwordEncoder, operationLogService)
                .assignRoles(1L, request);

        verify(userRoleMapper).deleteByUserId(1L);
        verify(userRoleMapper).insertRelation(1L, 10L);
        verify(userRoleMapper).insertRelation(1L, 20L);
        verify(operationLogService).record("system", "User", 1L, "ASSIGN_ROLES", "Assign roles to user 1");
    }
}
