package com.jitong.projectflow.system.service;

import com.jitong.projectflow.common.error.BusinessException;
import com.jitong.projectflow.system.audit.OperationLogService;
import com.jitong.projectflow.system.dto.RoleCreateRequest;
import com.jitong.projectflow.system.dto.RoleMenuAssignRequest;
import com.jitong.projectflow.system.entity.RoleEntity;
import com.jitong.projectflow.system.entity.SystemUser;
import com.jitong.projectflow.system.entity.UserRoleEntity;
import com.jitong.projectflow.system.mapper.RoleMapper;
import com.jitong.projectflow.system.mapper.RoleMenuMapper;
import com.jitong.projectflow.system.mapper.SystemUserMapper;
import com.jitong.projectflow.system.mapper.UserRoleMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleManagementServiceTest {
    @Mock
    RoleMapper roleMapper;
    @Mock
    RoleMenuMapper roleMenuMapper;
    @Mock
    UserRoleMapper userRoleMapper;
    @Mock
    SystemUserMapper systemUserMapper;
    @Mock
    OperationLogService operationLogService;

    @Test
    void createRejectsDuplicateCode() {
        when(roleMapper.selectCount(any())).thenReturn(1L);
        RoleCreateRequest request = new RoleCreateRequest();
        request.setCode("ADMIN");
        request.setName("Admin");

        assertThatThrownBy(() -> new RoleManagementService(roleMapper, roleMenuMapper, userRoleMapper, systemUserMapper, operationLogService).create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("角色编码已存在");
    }

    @Test
    void assignMenusReplacesExistingRows() {
        when(roleMapper.selectById(1L)).thenReturn(new RoleEntity());
        RoleMenuAssignRequest request = new RoleMenuAssignRequest();
        request.setMenuIds(List.of(100L, 200L));

        new RoleManagementService(roleMapper, roleMenuMapper, userRoleMapper, systemUserMapper, operationLogService)
                .assignMenus(1L, request);

        verify(roleMenuMapper).deleteByRoleId(1L);
        verify(roleMenuMapper).insertRelation(1L, 100L);
        verify(roleMenuMapper).insertRelation(1L, 200L);
    }

    @Test
    void deleteBlockedWhenAssignedToActiveUser() {
        RoleEntity role = new RoleEntity();
        role.setId(1L);
        role.setCode("ADMIN");
        role.setName("Admin");
        when(roleMapper.selectById(1L)).thenReturn(role);
        UserRoleEntity userRole = new UserRoleEntity();
        userRole.setUserId(10L);
        userRole.setRoleId(1L);
        when(userRoleMapper.selectByRoleId(1L)).thenReturn(List.of(userRole));
        SystemUser user = new SystemUser();
        user.setEnabled(true);
        when(systemUserMapper.selectById(10L)).thenReturn(user);

        assertThatThrownBy(() -> new RoleManagementService(roleMapper, roleMenuMapper, userRoleMapper, systemUserMapper, operationLogService).delete(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("角色已分配给活跃用户，无法删除");
    }
}
