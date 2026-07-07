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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

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

        var response = new CurrentUserPermissionService(systemUserMapper, userRoleMapper, roleMapper, roleMenuMapper, menuMapper).getCurrentUser();

        assertThat(response.getUsername()).isEqualTo("admin");
        assertThat(response.getRoles()).extracting("code").containsExactly("ADMIN");
        assertThat(response.getMenus()).extracting("code").containsExactly("system:user");
        assertThat(response.getPermissions()).containsExactly("system:user");
    }
}
