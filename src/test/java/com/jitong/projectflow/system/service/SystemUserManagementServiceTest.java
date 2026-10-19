package com.jitong.projectflow.system.service;

import com.jitong.projectflow.auth.security.CurrentUserContext;
import com.jitong.projectflow.auth.security.BusinessAccessService;
import com.jitong.projectflow.common.error.BusinessException;
import com.jitong.projectflow.system.audit.OperationLogService;
import com.jitong.projectflow.system.dto.UserCreateRequest;
import com.jitong.projectflow.system.dto.UserEnabledUpdateRequest;
import com.jitong.projectflow.system.dto.UserRoleAssignRequest;
import com.jitong.projectflow.system.dto.UserUpdateRequest;
import com.jitong.projectflow.system.entity.SystemUser;
import com.jitong.projectflow.system.entity.RoleEntity;
import com.jitong.projectflow.system.entity.DepartmentEntity;
import com.jitong.projectflow.system.mapper.SystemUserMapper;
import com.jitong.projectflow.system.mapper.RoleMapper;
import com.jitong.projectflow.system.mapper.DepartmentMapper;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
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
    @Mock
    RoleMapper roleMapper;
    @Mock
    BusinessAccessService businessAccessService;
    @Mock
    DepartmentMapper departmentMapper;

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

        new SystemUserManagementService(systemUserMapper, userRoleMapper, passwordEncoder, operationLogService, roleMapper, businessAccessService, departmentMapper)
                .create(request);

        ArgumentCaptor<SystemUser> captor = ArgumentCaptor.forClass(SystemUser.class);
        verify(systemUserMapper).insert(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("hashed");
        assertThat(captor.getValue().getCreatedBy()).isEqualTo(99L);
        verify(operationLogService).record("system", "User", captor.getValue().getId(), "CREATE", "新建用户：Li Si（lisi）");
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

        var response = new SystemUserManagementService(systemUserMapper, userRoleMapper, passwordEncoder, operationLogService, roleMapper, businessAccessService, departmentMapper)
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
        user.setRealName("Li Si");
        user.setEnabled(true);
        when(systemUserMapper.selectById(1L)).thenReturn(user);
        UserEnabledUpdateRequest request = new UserEnabledUpdateRequest();
        request.setEnabled(false);

        new SystemUserManagementService(systemUserMapper, userRoleMapper, passwordEncoder, operationLogService, roleMapper, businessAccessService, departmentMapper)
                .updateEnabled(1L, request);

        assertThat(user.getEnabled()).isFalse();
        verify(systemUserMapper).updateById(user);
        verify(operationLogService).record("system", "User", 1L, "ENABLE_CHANGED", "停用用户：Li Si（lisi）");
    }

    @Test
    void assignRolesReplacesExistingRows() {
        SystemUser user = new SystemUser();
        user.setRealName("Li Si");
        when(systemUserMapper.selectById(1L)).thenReturn(user);
        UserRoleAssignRequest request = new UserRoleAssignRequest();
        request.setRoleIds(List.of(10L, 20L));

        new SystemUserManagementService(systemUserMapper, userRoleMapper, passwordEncoder, operationLogService, roleMapper, businessAccessService, departmentMapper)
                .assignRoles(1L, request);

        verify(userRoleMapper).deleteByUserId(1L);
        verify(userRoleMapper).insertRelation(1L, 10L);
        verify(userRoleMapper).insertRelation(1L, 20L);
        verify(operationLogService).record("system", "User", 1L, "ASSIGN_ROLES", "为用户「Li Si」分配角色，共 2 个");
    }

    @Test
    void delegatedUserEditorCannotDisableAdministrator() {
        SystemUser user = new SystemUser();
        user.setId(1L);
        user.setEnabled(true);
        when(systemUserMapper.selectById(1L)).thenReturn(user);
        when(userRoleMapper.selectRoleIdsByUserId(1L)).thenReturn(List.of(10L));
        RoleEntity role = new RoleEntity();
        role.setCode("admin");
        when(roleMapper.selectById(10L)).thenReturn(role);
        UserEnabledUpdateRequest request = new UserEnabledUpdateRequest();
        request.setEnabled(false);

        assertThatThrownBy(() -> new SystemUserManagementService(systemUserMapper, userRoleMapper, passwordEncoder,
                operationLogService, roleMapper, businessAccessService, departmentMapper).updateEnabled(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("管理员账号");
        verify(systemUserMapper, never()).updateById(any(SystemUser.class));
    }

    @Test
    void delegatedUserManagerCannotGrantGmOfficeReadScope() {
        DepartmentEntity gmOffice = new DepartmentEntity();
        gmOffice.setName("总经办");
        when(departmentMapper.selectById(5L)).thenReturn(gmOffice);
        UserCreateRequest create = new UserCreateRequest();
        create.setDepartmentId(5L);
        assertThatThrownBy(() -> new SystemUserManagementService(systemUserMapper, userRoleMapper, passwordEncoder,
                operationLogService, roleMapper, businessAccessService, departmentMapper).create(create))
                .isInstanceOf(BusinessException.class).hasMessageContaining("总经办");

        SystemUser user = new SystemUser();
        user.setId(1L);
        user.setDepartmentId(2L);
        when(systemUserMapper.selectById(1L)).thenReturn(user);
        UserUpdateRequest update = new UserUpdateRequest();
        update.setDepartmentId(5L);
        assertThatThrownBy(() -> new SystemUserManagementService(systemUserMapper, userRoleMapper, passwordEncoder,
                operationLogService, roleMapper, businessAccessService, departmentMapper).update(1L, update))
                .isInstanceOf(BusinessException.class).hasMessageContaining("总经办");
        verify(systemUserMapper, never()).insert(any(SystemUser.class));
        verify(systemUserMapper, never()).updateById(any(SystemUser.class));
    }

    @Test
    void delegatedUserEditorCannotDisableGmOfficeAccount() {
        SystemUser gmUser = new SystemUser();
        gmUser.setId(1L);
        gmUser.setDepartmentId(5L);
        when(systemUserMapper.selectById(1L)).thenReturn(gmUser);
        DepartmentEntity gmOffice = new DepartmentEntity();
        gmOffice.setName("总经办");
        when(departmentMapper.selectById(5L)).thenReturn(gmOffice);
        UserEnabledUpdateRequest request = new UserEnabledUpdateRequest();
        request.setEnabled(false);

        assertThatThrownBy(() -> new SystemUserManagementService(systemUserMapper, userRoleMapper, passwordEncoder,
                operationLogService, roleMapper, businessAccessService, departmentMapper).updateEnabled(1L, request))
                .isInstanceOf(BusinessException.class).hasMessageContaining("总经办账号");
        verify(systemUserMapper, never()).updateById(any(SystemUser.class));
    }
}
