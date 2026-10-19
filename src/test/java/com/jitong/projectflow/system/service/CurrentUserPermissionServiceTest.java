package com.jitong.projectflow.system.service;

import com.jitong.projectflow.auth.security.CurrentUserContext;
import com.jitong.projectflow.system.entity.MenuEntity;
import com.jitong.projectflow.system.entity.RoleEntity;
import com.jitong.projectflow.system.entity.SystemUser;
import com.jitong.projectflow.system.mapper.MenuMapper;
import com.jitong.projectflow.system.mapper.RoleMapper;
import com.jitong.projectflow.system.mapper.RoleMenuMapper;
import com.jitong.projectflow.system.mapper.SystemUserMapper;
import com.jitong.projectflow.system.mapper.UserRoleMapper;
import com.jitong.projectflow.system.mapper.DepartmentMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class CurrentUserPermissionServiceTest {
    @Mock
    SystemUserMapper systemUserMapper;
    @Mock
    UserRoleMapper userRoleMapper;
    @Mock
    RoleMapper roleMapper;
    @Mock
    RoleMenuMapper roleMenuMapper;
    @Mock
    MenuMapper menuMapper;
    @Mock
    DepartmentMapper departmentMapper;

    @AfterEach
    void clearCurrentUser() {
        CurrentUserContext.clear();
    }

    @Test
    void getCurrentUserProfileDeduplicatesPermissionCodes() {
        CurrentUserContext.set(1L);
        SystemUser user = new SystemUser();
        user.setId(1L);
        user.setUsername("admin");
        user.setRealName("Admin");
        user.setEnabled(true);
        RoleEntity role = new RoleEntity();
        role.setId(10L);
        role.setCode("ADMIN");
        role.setName("Admin");
        role.setEnabled(true);
        MenuEntity menu = new MenuEntity();
        menu.setId(100L);
        menu.setCode("system:user");
        menu.setName("User");
        menu.setType("MENU");
        when(systemUserMapper.selectById(1L)).thenReturn(user);
        when(userRoleMapper.selectRoleIdsByUserId(1L)).thenReturn(List.of(10L));
        when(roleMapper.selectList(any())).thenReturn(List.of(role));
        when(roleMenuMapper.selectMenuIdsByRoleIds(List.of(10L))).thenReturn(List.of(100L, 100L));
        when(menuMapper.selectList(any())).thenReturn(List.of(menu));

        var response = new CurrentUserPermissionService(systemUserMapper, userRoleMapper, roleMapper, roleMenuMapper, menuMapper, departmentMapper).getCurrentUser();

        assertThat(response.getUsername()).isEqualTo("admin");
        assertThat(response.getRoles()).extracting("code").containsExactly("ADMIN");
        assertThat(response.getMenus()).extracting("code").containsExactly("system:user");
        assertThat(response.getPermissions()).containsExactly("system:user");
    }

    @Test
    void getPermissionsByUserIdDeduplicatesPermissionCodes() {
        SystemUser user = new SystemUser();
        user.setId(1L);
        user.setUsername("admin");
        user.setEnabled(true);
        MenuEntity menu = new MenuEntity();
        menu.setId(100L);
        menu.setCode("system:user:view");
        menu.setName("View Users");
        menu.setType("BUTTON");
        RoleEntity role = new RoleEntity();
        role.setId(10L);
        role.setCode("STAFF");
        role.setEnabled(true);
        when(systemUserMapper.selectById(1L)).thenReturn(user);
        when(userRoleMapper.selectRoleIdsByUserId(1L)).thenReturn(List.of(10L));
        when(roleMapper.selectList(any())).thenReturn(List.of(role));
        when(roleMenuMapper.selectMenuIdsByRoleIds(List.of(10L))).thenReturn(List.of(100L, 100L));
        when(menuMapper.selectList(any())).thenReturn(List.of(menu, menu));

        var permissions = new CurrentUserPermissionService(systemUserMapper, userRoleMapper, roleMapper, roleMenuMapper, menuMapper, departmentMapper)
                .getPermissionsByUserId(1L);

        assertThat(permissions).containsExactly("system:user:view");
    }

    @Test
    void enabledRolesContributeRoleAuthoritiesSeparatelyFromActionCodes() {
        SystemUser user = new SystemUser();
        user.setId(1L);
        user.setEnabled(true);
        RoleEntity role = new RoleEntity();
        role.setId(10L);
        role.setCode("ADMIN");
        role.setEnabled(true);
        when(systemUserMapper.selectById(1L)).thenReturn(user);
        when(userRoleMapper.selectRoleIdsByUserId(1L)).thenReturn(List.of(10L));
        when(roleMapper.selectList(any())).thenReturn(List.of(role));

        var authorities = new CurrentUserPermissionService(systemUserMapper, userRoleMapper, roleMapper,
                roleMenuMapper, menuMapper, departmentMapper).getAuthoritiesByUserId(1L);

        assertThat(authorities).containsExactly("ROLE_ADMIN");
    }

    @Test
    void assignedRolesWithoutEnabledRowsGrantNoAuthoritiesOrMenuAccess() {
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(
                new org.apache.ibatis.builder.MapperBuilderAssistant(new com.baomidou.mybatisplus.core.MybatisConfiguration(), "permission-test"),
                RoleEntity.class);
        SystemUser user = new SystemUser();
        user.setId(1L);
        user.setEnabled(true);
        when(systemUserMapper.selectById(1L)).thenReturn(user);
        when(userRoleMapper.selectRoleIdsByUserId(1L)).thenReturn(List.of(10L));
        when(roleMapper.selectList(any())).thenAnswer(invocation -> {
            var wrapper = invocation.getArgument(0, com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper.class);
            assertThat(wrapper.getSqlSegment()).contains("enabled");
            assertThat(wrapper.getParamNameValuePairs()).containsValue(true);
            return List.of();
        });

        var authorities = new CurrentUserPermissionService(systemUserMapper, userRoleMapper, roleMapper,
                roleMenuMapper, menuMapper, departmentMapper).getAuthoritiesByUserId(1L);

        assertThat(authorities).isEmpty();
        verify(roleMapper).selectList(any());
        verifyNoInteractions(roleMenuMapper, menuMapper);
    }
}
